package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 探店博客接口，提供发布、点赞、查询和关注流功能
 */
@RestController
@RequestMapping("/blog")
@Slf4j
public class BlogController {

    @Resource
    private IBlogService blogService;

    /** 发布探店博客 */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        log.info("保存博客:{}", blog);
        return blogService.saveBlog(blog);
    }

    /** 点赞博客 */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        log.info("点赞博客:{}", id);
        return blogService.likeBlog(id);
    }

    /** 查询当前用户发布的博客 */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        log.info("查询当前用户发布的博客");
        return blogService.queryMyBlog(current);
    }

    /** 查询热门博客 */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        log.info("查询热门博客");
        return blogService.queryHotBlog(current);
    }

    /** 根据ID查询博客详情 */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        log.info("根据id查询博客详情:{}", id);
        return blogService.queryBlogById(id);
    }

    /** 查询博客点赞用户 */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        log.info("查询博客点赞数:{}", id);
        return blogService.queryBlogLikes(id);
    }

    /** 查询指定用户的博客列表 */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "id") Long id) {
        log.info("查询用户id:{}的博客列表", id);
        Page<Blog> page = blogService.query().eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

    /** 查询关注用户的博客（滚动分页） */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        log.info("查询用户关注列表的博客列表");
        return blogService.queryBlogOfFollow(max, offset);
    }
}
