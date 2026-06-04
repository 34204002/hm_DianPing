package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.ai.SearchResponse;
import com.hmdp.service.IAISearchService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AISearchServiceImpl implements IAISearchService {

    @Autowired
    private ChatClient searchChatClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public SearchResponse search(String query, Double userX, Double userY) {
        String coordPart = (userX != null && userY != null) ? userX + ":" + userY : "";
        String cacheKey = RedisConstants.AI_SEARCH_KEY + DigestUtil.md5Hex(query + ":" + coordPart);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return JSONUtil.toBean(cached, SearchResponse.class);
        }

        try {
            String aiResponse = searchChatClient.prompt()
                    .user(query)
                    .call()
                    .content();

            SearchResponse response = new SearchResponse(Collections.emptyList(), aiResponse);
            stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(response), 1, TimeUnit.HOURS);
            return response;
        } catch (Exception e) {
            log.warn("AI搜索失败: query={}", query, e);
            return new SearchResponse(Collections.emptyList(), "AI搜索暂时不可用，请稍后再试");
        }
    }
}
