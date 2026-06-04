package com.hmdp.controller;

import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.ai.SearchRequest;
import com.hmdp.dto.ai.SearchResponse;
import com.hmdp.service.IAISearchService;
import com.hmdp.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI智能搜索接口
 */
@RestController
@RequestMapping("/ai")
public class AISearchController {

    @Autowired
    private IAISearchService aiSearchService;

    /** 自然语言搜索商户 */
    @PostMapping("/search")
    public Result search(@RequestBody SearchRequest request) {
        if (request == null || StrUtil.isBlank(request.getQuery())) {
            return Result.fail("搜索内容不能为空");
        }
        SearchResponse response = aiSearchService.search(request.getQuery(), request.getX(), request.getY());
        return Result.ok(response);
    }
}
