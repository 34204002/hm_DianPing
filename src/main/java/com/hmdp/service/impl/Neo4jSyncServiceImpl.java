package com.hmdp.service.impl;

import com.hmdp.entity.BlogNode;
import com.hmdp.entity.ShopNode;
import com.hmdp.entity.UserNode;
import com.hmdp.repository.BlogNodeRepository;
import com.hmdp.repository.Neo4jCypherRepository;
import com.hmdp.repository.ShopNodeRepository;
import com.hmdp.repository.UserNodeRepository;
import com.hmdp.service.INeo4jSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Neo4j数据同步服务，负责将MySQL的关注、博客发布、点赞操作同步到Neo4j图数据库
 */
@Slf4j
@Service
public class Neo4jSyncServiceImpl implements INeo4jSyncService {

    private final Neo4jCypherRepository cypherRepository;
    private final UserNodeRepository userNodeRepository;
    private final BlogNodeRepository blogNodeRepository;
    private final ShopNodeRepository shopNodeRepository;

    public Neo4jSyncServiceImpl(Neo4jCypherRepository cypherRepository,
                            UserNodeRepository userNodeRepository,
                            BlogNodeRepository blogNodeRepository,
                            ShopNodeRepository shopNodeRepository) {
        this.cypherRepository = cypherRepository;
        this.userNodeRepository = userNodeRepository;
        this.blogNodeRepository = blogNodeRepository;
        this.shopNodeRepository = shopNodeRepository;
    }

    public void syncFollow(Long userId, Long followUserId, boolean isFollow) {
        log.info("Neo4j同步关注开始: userId={}, followUserId={}, isFollow={}", userId, followUserId, isFollow);
        try {
            ensureUserNode(userId);
            ensureUserNode(followUserId);
            if (isFollow) {
                cypherRepository.createFollowRelationship(userId, followUserId);
                log.info("Neo4j关注关系创建完成");
            } else {
                cypherRepository.deleteFollowRelationship(userId, followUserId);
                log.info("Neo4j关注关系删除完成");
            }
        } catch (Exception e) {
            log.warn("Neo4j同步关注关系失败: userId={}, followUserId={}, isFollow={}", userId, followUserId, isFollow, e);
        }
    }

    public void syncBlogWrite(Long userId, Long blogId, Long shopId) {
        try {
            ensureUserNode(userId);
            ensureBlogNode(blogId);
            if (shopId != null) {
                ensureShopNode(shopId, null, null, null, null);
                cypherRepository.createWroteRelationship(userId, blogId);
                cypherRepository.createAboutRelationship(blogId, shopId);
            } else {
                cypherRepository.createWroteRelationship(userId, blogId);
            }
        } catch (Exception e) {
            log.warn("Neo4j同步博客发布失败: userId={}, blogId={}, shopId={}", userId, blogId, shopId, e);
        }
    }

    public void syncBlogLike(Long userId, Long shopId, boolean isLike) {
        if (shopId == null) return;
        try {
            ensureUserNode(userId);
            ensureShopNode(shopId, null, null, null, null);
            if (isLike) {
                cypherRepository.createLikesRelationship(userId, shopId);
            } else {
                cypherRepository.deleteLikesRelationship(userId, shopId);
            }
        } catch (Exception e) {
            log.warn("Neo4j同步点赞关系失败: userId={}, shopId={}", userId, shopId, e);
        }
    }

    public void syncShopCreate(Long shopId, String name, Long typeId, Long avgPrice, Integer score) {
        try {
            ensureShopNode(shopId, name, typeId, avgPrice, score);
        } catch (Exception e) {
            log.warn("Neo4j同步商铺创建失败: shopId={}", shopId, e);
        }
    }

    private void ensureUserNode(Long userId) {
        userNodeRepository.findByUserId(userId).orElseGet(() ->
                userNodeRepository.save(UserNode.builder().userId(userId).nickname(null).build()));
    }

    private void ensureBlogNode(Long blogId) {
        blogNodeRepository.findByBlogId(blogId).orElseGet(() ->
                blogNodeRepository.save(BlogNode.builder().blogId(blogId).build()));
    }

    private void ensureShopNode(Long shopId, String name, Long typeId, Long avgPrice, Integer score) {
        shopNodeRepository.findByShopId(shopId).orElseGet(() ->
                shopNodeRepository.save(ShopNode.builder()
                        .shopId(shopId)
                        .name(name)
                        .typeId(typeId)
                        .avgPrice(avgPrice)
                        .score(score)
                        .build()));
    }
}
