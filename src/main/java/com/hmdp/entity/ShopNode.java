package com.hmdp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Shop")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopNode {
    @Id
    @GeneratedValue
    private Long id;

    @Property("shopId")
    private Long shopId;

    @Property("name")
    private String name;

    @Property("typeId")
    private Long typeId;

    @Property("avgPrice")
    private Long avgPrice;

    @Property("score")
    private Integer score;
}
