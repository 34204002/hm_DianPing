package com.hmdp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Blog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogNode {
    @Id
    @GeneratedValue
    private Long id;

    @Property("blogId")
    private Long blogId;
}
