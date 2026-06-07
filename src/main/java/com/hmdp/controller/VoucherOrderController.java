package com.hmdp.controller;

import com.hmdp.annotation.RateLimiter;
import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券秒杀订单接口
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Autowired
    private IVoucherOrderService voucherOrderService;

    /** 秒杀优惠券 */
    @PostMapping("seckill/{id}")
    @RateLimiter(key = "seckill", permits = 50, period = 1, message = "秒杀太火爆，请稍后再试")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
