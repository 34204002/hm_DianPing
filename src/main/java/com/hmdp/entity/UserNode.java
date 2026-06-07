package com.hmdp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("User")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNode {
    @Id
    @GeneratedValue
    private Long id;

    @Property("userId")
    private Long userId;

    @Property("nickname")
    private String nickname;
}
