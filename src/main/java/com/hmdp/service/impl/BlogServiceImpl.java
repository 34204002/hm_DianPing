package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
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
