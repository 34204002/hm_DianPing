package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
@Slf4j
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        log.info("查询所有店铺分类");
        List<ShopType> typeList = typeService.queryTypeList();
        if (typeList == null)
            return Result.fail("查询失败");
        return Result.ok(typeList.stream().sorted(Comparator.comparingInt(ShopType::getSort)).toList());
    }
}