package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private IUserService userService;
    @Autowired
    private IFollowService followService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        //查询博客数据
        Blog blog = getById(id);
        //查询用户数据
        User user = userService.getById(blog.getUserId());
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
        setBlogIsLike(blog);
        setBlogUser(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        });
        setBlogIsLike(records);
        setBlogUser(records);
        return Result.ok(records);
    }

    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        setBlogIsLike(records);
        setBlogUser(records);
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        //获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 判断当前用户是否已经点赞
        String key=RedisConstants.BLOG_LIKED_KEY+id;
        // 如果未点赞
        if(!isLike(id)) {
            // 点赞数 + 1
            update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户点赞信息到redis的zset集合
            stringRedisTemplate.opsForZSet().add(key,String.valueOf(userId),System.currentTimeMillis());
        }
        // 如果已经点赞
        else {
            // 点赞数-1
            update().setSql("liked = liked - 1").eq("id", id).update();
            // 从redis的set集合中移除当前用户
            stringRedisTemplate.opsForZSet().remove(key, String.valueOf(userId));
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        //查询top5的点赞用户
        String key=RedisConstants.BLOG_LIKED_KEY+id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5==null||top5.isEmpty())
            return Result.ok();
        List<Long> ids = top5.stream().map(Long::valueOf).toList();
        String join = StrUtil.join(",", ids);
        List<UserDTO> users=userService.query()
                .in("id", ids)
                .last("order by FIELD(id,"+join+")")
                .list()
                .stream()
                .map(user -> new UserDTO(user.getId(), user.getNickName(), user.getIcon()))
                .toList();
        return Result.ok(users);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if (!isSuccess)
            return Result.fail("发布失败");
        // 查询笔记作者粉丝
        List<Long> fansIds = followService.query().eq("follow_user_id", user.getId()).list()
                .stream()
                .map(Follow::getUserId)
                .toList();
        // 推送笔记id给粉丝
        List<String> fansKey = fansIds.stream().map(fansId -> RedisConstants.FEED_KEY + fansId).toList();
        for (String s : fansKey) {
            stringRedisTemplate.opsForZSet().add(s,String.valueOf(blog.getId()), System.currentTimeMillis());
        }

        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        //查询用户收件箱
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(RedisConstants.FEED_KEY + userId, 0, max, offset, 3);
        //解析数据: blogId,minTime(时间戳),offset
        if(typedTuples==null||typedTuples.isEmpty())
            return Result.ok();
        List<Long> blogIds=new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            blogIds.add(Long.valueOf(typedTuple.getValue()));
            long time = typedTuple.getScore().longValue();
            if (time == minTime)
                os++;
            else {
                minTime = time;
                os = 1;
            }
        }
        //根据blogId查询blog
        String joinStr = StrUtil.join(",", blogIds);
        List<Blog> blogs = query().in("id", blogIds).last("ORDER BY FIELD(id," + joinStr + ")").list();
        setBlogIsLike(blogs);
        setBlogUser(blogs);
        //封装数据
        ScrollResult scrollResult = new ScrollResult(blogs, minTime, os);

        //返回数据
        return Result.ok(scrollResult);
    }

    private void setBlogIsLike(Blog blog){
        if(BooleanUtil.isTrue(isLike(blog.getId()))){
            blog.setIsLike(true);
        }else {
            blog.setIsLike(false);
        }
    }
    private void setBlogIsLike(List<Blog> blogList){
        for (Blog blog : blogList) {
               if (BooleanUtil.isTrue(isLike(blog.getId()))) {
                blog.setIsLike(true);
            } else {
                blog.setIsLike(false);
            }
        }
    }
    private void setBlogUser(Blog blog){
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
    private void setBlogUser(List<Blog> blogList){
        for (Blog blog : blogList) {
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
    }
    private boolean isLike(Long blogId){
        boolean isLike = false;
        //已登录
        if(UserHolder.getUser()!=null){
            Double score = stringRedisTemplate.opsForZSet().score(RedisConstants.BLOG_LIKED_KEY + blogId, String.valueOf(UserHolder.getUser().getId()));
            if (score != null)
                isLike = true;
        }
        return isLike;
    }
}
