package com.hmdp.repository;

import com.hmdp.entity.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
    Optional<UserNode> findByUserId(Long userId);
}
