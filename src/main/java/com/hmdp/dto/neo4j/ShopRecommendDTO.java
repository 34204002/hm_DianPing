package com.hmdp.dto.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopRecommendDTO {
    private Long shopId;
    private String name;
    private Long avgPrice;
    private Integer score;
    private Integer friendCount;
}
