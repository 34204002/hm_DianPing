package com.hmdp.repository;

import com.hmdp.entity.BlogNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface BlogNodeRepository extends Neo4jRepository<BlogNode, Long> {
    Optional<BlogNode> findByBlogId(Long blogId);
}
