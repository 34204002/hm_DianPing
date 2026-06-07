package com.hmdp.repository;

import com.hmdp.entity.ShopNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface ShopNodeRepository extends Neo4jRepository<ShopNode, Long> {
    Optional<ShopNode> findByShopId(Long shopId);
}
