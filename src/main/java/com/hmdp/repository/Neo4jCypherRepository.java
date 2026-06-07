package com.hmdp.repository;

import com.hmdp.dto.neo4j.KnownUserDTO;
import com.hmdp.dto.neo4j.ShopRecommendDTO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;

import org.neo4j.driver.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class Neo4jCypherRepository {

    private final Driver neo4jDriver;

    public Neo4jCypherRepository(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    // ==================== 关注关系 ====================

    public void createFollowRelationship(Long userId, Long followUserId) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (u:User {userId: $userId})
                    MATCH (f:User {userId: $followUserId})
                    MERGE (u)-[:FOLLOWS]->(f)
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId, "followUserId", followUserId));
        }
    }

    public void deleteFollowRelationship(Long userId, Long followUserId) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (u:User {userId: $userId})-[r:FOLLOWS]->(f:User {userId: $followUserId})
                    DELETE r
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId, "followUserId", followUserId));
        }
    }

    // ==================== 博客关系 ====================

    public void createWroteRelationship(Long userId, Long blogId) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (u:User {userId: $userId})
                    MATCH (b:Blog {blogId: $blogId})
                    MERGE (u)-[:WROTE]->(b)
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId, "blogId", blogId));
        }
    }

    public void createAboutRelationship(Long blogId, Long shopId) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (b:Blog {blogId: $blogId})
                    MATCH (s:Shop {shopId: $shopId})
                    MERGE (b)-[:ABOUT]->(s)
                    """,
                    org.neo4j.driver.Values.parameters("blogId", blogId, "shopId", shopId));
        }
    }

    // ==================== 点赞关系 ====================

    public void createLikesRelationship(Long userId, Long shopId) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (u:User {userId: $userId})
                    MATCH (s:Shop {shopId: $shopId})
                    MERGE (u)-[:LIKES]->(s)
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId, "shopId", shopId));
        }
    }

    public void deleteLikesRelationship(Long userId, Long shopId) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (u:User {userId: $userId})-[r:LIKES]->(s:Shop {shopId: $shopId})
                    DELETE r
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId, "shopId", shopId));
        }
    }

    // ==================== 推荐查询 ====================

    public List<ShopRecommendDTO> findShopsFriendsVisited(Long userId) {
        try (Session session = neo4jDriver.session()) {
            List<Record> records = session.run("""
                    MATCH (u:User {userId: $userId})-[:FOLLOWS]->(:User)-[:WROTE]->(:Blog)-[:ABOUT]->(s:Shop)
                    RETURN DISTINCT s.shopId AS shopId, s.name AS name, s.avgPrice AS avgPrice,
                           s.score AS score, COUNT(*) AS friendCount
                    ORDER BY friendCount DESC LIMIT 20
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId)).list();

            List<ShopRecommendDTO> result = new ArrayList<>();
            for (Record r : records) {
                result.add(mapToShopDTO(r));
            }
            return result;
        }
    }

    public List<ShopRecommendDTO> findShopsFriendsLiked(Long userId) {
        try (Session session = neo4jDriver.session()) {
            List<Record> records = session.run("""
                    MATCH (u:User {userId: $userId})-[:FOLLOWS]->(:User)-[:LIKES]->(s:Shop)
                    RETURN DISTINCT s.shopId AS shopId, s.name AS name, s.avgPrice AS avgPrice,
                           s.score AS score, COUNT(*) AS friendCount
                    ORDER BY friendCount DESC LIMIT 20
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId)).list();

            List<ShopRecommendDTO> result = new ArrayList<>();
            for (Record r : records) {
                result.add(mapToShopDTO(r));
            }
            return result;
        }
    }

    public List<KnownUserDTO> findPeopleYouMightKnow(Long userId, int limit) {
        try (Session session = neo4jDriver.session()) {
            List<Record> records = session.run("""
                    MATCH (u:User {userId: $userId})-[:LIKES]->(s:Shop)<-[:LIKES]-(other:User)
                    WHERE NOT (u)-[:FOLLOWS]->(other) AND other.userId <> $userId
                    RETURN DISTINCT other.userId AS userId, other.nickname AS nickname,
                           COUNT(s) AS commonCount, COLLECT(s.name) AS commonShops
                    ORDER BY commonCount DESC LIMIT $limit
                    """,
                    org.neo4j.driver.Values.parameters("userId", userId, "limit", limit)).list();

            List<KnownUserDTO> result = new ArrayList<>();
            for (Record r : records) {
                result.add(KnownUserDTO.builder()
                        .userId(r.get("userId").asLong())
                        .nickname(nullSafeString(r.get("nickname")))
                        .commonCount(r.get("commonCount").asInt())
                        .commonShops(r.get("commonShops").asList(v -> nullSafeString(v)))
                        .build());
            }
            return result;
        }
    }

    /** 查询两个用户的共同关注用户ID列表 */
    public List<Long> findCommonFollows(Long userId1, Long userId2) {
        try (Session session = neo4jDriver.session()) {
            List<Record> records = session.run("""
                    MATCH (u1:User {userId: $userId1})-[:FOLLOWS]->(common:User)<-[:FOLLOWS]-(u2:User {userId: $userId2})
                    RETURN common.userId AS userId
                    """,
                    org.neo4j.driver.Values.parameters("userId1", userId1, "userId2", userId2)).list();

            List<Long> result = new ArrayList<>();
            for (Record r : records) {
                result.add(r.get("userId").asLong());
            }
            return result;
        }
    }

    private ShopRecommendDTO mapToShopDTO(Record r) {
        return ShopRecommendDTO.builder()
                .shopId(r.get("shopId").asLong())
                .name(nullSafeString(r.get("name")))
                .avgPrice(nullSafeLong(r.get("avgPrice")))
                .score(nullSafeInt(r.get("score")))
                .friendCount(r.get("friendCount").asInt())
                .build();
    }

    private String nullSafeString(Value v) {
        return v.isNull() ? null : v.asString();
    }

    private Long nullSafeLong(Value v) {
        return v.isNull() ? null : v.asLong();
    }

    private Integer nullSafeInt(Value v) {
        return v.isNull() ? null : v.asInt();
    }
}
