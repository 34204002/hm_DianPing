package com.hmdp.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.ai.ShopSummaryDTO;
import com.hmdp.dto.ai.ShopTypeDTO;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShopSearchTools {

    private final IShopTypeService shopTypeService;
    private final IShopService shopService;

    public ShopSearchTools(IShopTypeService shopTypeService, IShopService shopService) {
        this.shopTypeService = shopTypeService;
        this.shopService = shopService;
    }

    @Tool(description = "获取所有可用的商户类型（菜系）列表")
    public List<ShopTypeDTO> getShopTypes() {
        return shopTypeService.list().stream()
                .map(t -> new ShopTypeDTO(t.getId(), t.getName()))
                .toList();
    }

    @Tool(description = "根据条件搜索商户，所有参数可选")
    public List<ShopSummaryDTO> searchShops(
            @ToolParam(description = "商户类型ID") Long typeId,
            @ToolParam(description = "商户名称关键词") String keyword,
            @ToolParam(description = "商圈，如陆家嘴、三里屯") String area,
            @ToolParam(description = "最低评分，1-5") Double minScore,
            @ToolParam(description = "最高人均价格（元）") Long maxPrice,
            @ToolParam(description = "返回数量上限，默认10") Integer limit) {

        var wrapper = new LambdaQueryWrapper<Shop>();
        wrapper.select(Shop::getId, Shop::getName, Shop::getAvgPrice, Shop::getScore, Shop::getArea);

        if (typeId != null) {
            wrapper.eq(Shop::getTypeId, typeId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Shop::getName, keyword);
        }
        if (area != null && !area.isBlank()) {
            wrapper.like(Shop::getArea, area);
        }
        if (minScore != null) {
            wrapper.ge(Shop::getScore, (int) (minScore * 10));
        }
        if (maxPrice != null) {
            wrapper.le(Shop::getAvgPrice, maxPrice);
        }

        int size = limit != null ? Math.min(limit, 20) : 10;
        wrapper.last("LIMIT " + size);

        return shopService.list(wrapper).stream()
                .map(s -> ShopSummaryDTO.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .avgPrice(s.getAvgPrice())
                        .score(s.getScore())
                        .area(s.getArea())
                        .build())
                .toList();
    }
}
