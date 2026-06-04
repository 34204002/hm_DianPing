package com.hmdp.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 商户接口，支持CRUD、地理位置搜索和UV统计
 */
@RestController
@RequestMapping("/shop")
@Slf4j
public class ShopController {

    @Resource
    public IShopService shopService;

    /** 根据ID查询商铺详情 */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return Result.ok(shopService.queryById(id));
    }

    /** 新增商铺 */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        shopService.save(shop);
        return Result.ok(shop.getId());
    }

    /** 更新商铺信息 */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        log.info("更新商铺信息:{}", shop);
        if (shop.getId() == null)
            return Result.fail("店铺id不能为空");
        shopService.updateShopById(shop);
        return Result.ok();
    }

    /** 按类型分页查询商铺 */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y) {
        return shopService.queryShopByType(typeId, current, x, y);
    }

    /** 按名称搜索商铺 */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

    /** 记录商铺UV */
    @PostMapping("/uv/{shopId}")
    public Result saveShopUv(@PathVariable("shopId") Long shopId) {
        return shopService.saveShopUv(shopId);
    }

    /** 查询商铺UV数量 */
    @GetMapping("/uv/{shopId}")
    public Result queryShopUv(@PathVariable("shopId") Long shopId) {
        return shopService.queryShopUv(shopId);
    }
}
