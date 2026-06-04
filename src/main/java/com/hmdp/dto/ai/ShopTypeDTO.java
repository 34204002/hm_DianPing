package com.hmdp.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * getShopTypes 工具函数的返回值
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopTypeDTO {

    @JsonProperty("id")
    @JsonPropertyDescription("类型ID")
    private Long id;

    @JsonProperty("name")
    @JsonPropertyDescription("类型名称")
    private String name;
}
