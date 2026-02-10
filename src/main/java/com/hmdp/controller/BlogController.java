package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 博客前端控制器
 * 提供探店博客相关的REST API接口，包括发布、点赞、查询等功能
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
@Slf4j
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 保存探店博客
     * @param blog 博客实体，包含标题、内容、封面图等信息
     * @return 保存成功的博客ID
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        log.info("保存博客:{}", blog);
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    /**
     * 给指定博客点赞
     * @param id 博客ID
     * @return 操作结果
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        log.info("点赞博客:{}", id);
        return blogService.likeBlog(id);
    }

    /**
     * 查询当前用户发布的博客
     * @param current 当前页码，默认为1
     * @return 分页的博客列表
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        log.info("查询当前用户发布的博客");
        return blogService.queryMyBlog(current);
    }

    /**
     * 查询热门博客（按点赞数排序）
     * @param current 当前页码，默认为1
     * @return 分页的热门博客列表
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        log.info("查询热门博客");
        return blogService.queryHotBlog(current);
    }
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        log.info("根据id查询博客详情:{}",id);
        return blogService.queryBlogById(id);
    }
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        log.info("查询博客点赞数:{}",id);
        return blogService.queryBlogLikes(id);
    }
}