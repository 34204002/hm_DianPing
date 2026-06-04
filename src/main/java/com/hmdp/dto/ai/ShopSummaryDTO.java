package com.hmdp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索工具返回的商户摘要，只包含展示需要的字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSummaryDTO {
    private Long id;
    private String name;
    private Long avgPrice;
    private Integer score;
    private String area;
}
