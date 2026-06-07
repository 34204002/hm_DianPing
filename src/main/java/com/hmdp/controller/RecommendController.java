package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.INeo4jRecommendService;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 社交推荐接口
 */
@RestController
@RequestMapping("/recommend")
public class RecommendController {

    private final INeo4jRecommendService neo4jRecommendService;

    public RecommendController(INeo4jRecommendService neo4jRecommendService) {
        this.neo4jRecommendService = neo4jRecommendService;
    }

    /** 好友去过的店 */
    @GetMapping("/shops/friends-visited")
    public Result shopsFriendsVisited() {
        Long userId = UserHolder.getUser().getId();
        return Result.ok(neo4jRecommendService.getShopsFriendsVisited(userId));
    }

    /** 好友赞过的店 */
    @GetMapping("/shops/friends-liked")
    public Result shopsFriendsLiked() {
        Long userId = UserHolder.getUser().getId();
        return Result.ok(neo4jRecommendService.getShopsFriendsLiked(userId));
    }

    /** 可能认识的人 */
    @GetMapping("/users/might-know")
    public Result usersMightKnow(@RequestParam(defaultValue = "10") int limit) {
        Long userId = UserHolder.getUser().getId();
        return Result.ok(neo4jRecommendService.getPeopleYouMightKnow(userId, limit));
    }
}
