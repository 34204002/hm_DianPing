package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * <p>
 * 商铺前端控制器
 * 提供商铺相关的REST API接口，包括新增、查询、更新等功能
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop")
@Slf4j
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id 商铺ID
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return Result.ok(shopService.queryById(id));
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据实体
     * @return 包含商铺ID的结果
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据实体
     * @return 操作结果
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        log.info("更新商铺信息:{}",shop);
        if (shop.getId() == null)
            return Result.fail("店铺id不能为空");
        shopService.updateShopById(shop);
        return Result.ok();
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型ID
     * @param current 当前页码，默认为1
     * @return 分页的商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x",required = false) Double x,
            @RequestParam(value = "y",required = false) Double y
    ) {
       return shopService.queryShopByType(typeId,current,x,y);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字，可选参数
     * @param current 当前页码，默认为1
     * @return 分页的商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 记录店铺UV（独立访客）
     * @param shopId 店铺ID
     * @return 操作结果
     */
    @PostMapping("/uv/{shopId}")
    public Result saveShopUv(@PathVariable("shopId") Long shopId) {
        return shopService.saveShopUv(shopId);
    }

    /**
     * 查询店铺UV数量
     * @param shopId 店铺ID
     * @return UV数量
     */
    @GetMapping("/uv/{shopId}")
    public Result queryShopUv(@PathVariable("shopId") Long shopId) {
        return shopService.queryShopUv(shopId);
    }
}