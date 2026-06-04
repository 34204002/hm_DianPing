package com.hmdp.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * searchShops 工具函数的入参，所有字段可选
 */
@Data
public class SearchShopsRequest {

    @JsonProperty("typeId")
    @JsonPropertyDescription("商户类型ID")
    private Long typeId;

    @JsonProperty("name")
    @JsonPropertyDescription("商户名称关键词")
    private String name;

    @JsonProperty("area")
    @JsonPropertyDescription("商圈，如陆家嘴、三里屯")
    private String area;

    @JsonProperty("minScore")
    @JsonPropertyDescription("最低评分，1-5")
    private Double minScore;

    @JsonProperty("maxPrice")
    @JsonPropertyDescription("最高人均价格（元）")
    private Long maxPrice;

    @JsonProperty("limit")
    @JsonPropertyDescription("返回数量上限，默认10，最大20")
    private Integer limit;
}
