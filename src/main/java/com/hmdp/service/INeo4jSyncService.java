package com.hmdp.service;

public interface INeo4jSyncService {

    void syncFollow(Long userId, Long followUserId, boolean isFollow);

    void syncBlogWrite(Long userId, Long blogId, Long shopId);

    void syncBlogLike(Long userId, Long shopId, boolean isLike);

    void syncShopCreate(Long shopId, String name, Long typeId, Long avgPrice, Integer score);
}
