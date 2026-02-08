package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.ILock;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private ILock ilock;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        //3.判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        //4.判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        //7.返回订单id
        Result result;
        if(!ilock.tryLock(RedisConstants.LOCK_ORDER_USER_KEY+UserHolder.getUser().getId(), RedisConstants.LOCK_ORDER_USER_TTL, TimeUnit.SECONDS)) {
            return Result.fail("请勿重复下单");
        }
        try{
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            result = proxy.createVoucherOrder(voucherId);
        }catch (Exception e) {
            return Result.fail(e.getMessage());
        }
        finally {
            ilock.unlock(RedisConstants.LOCK_ORDER_USER_KEY+UserHolder.getUser().getId());
        }
        return result;

    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createVoucherOrder(Long voucherId) {
        //一人一单
        Long userId = UserHolder.getUser().getId();
        if (query().eq("user_id", userId).eq("voucher_id", voucherId).count() > 0) {
            return Result.fail("一个人只能购买一张");
        }
        //5.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .gt("stock", 0)
                .eq("voucher_id", voucherId)
                .update();
        if (!success)
            return null;
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1 生成订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //6.2 用户id
        voucherOrder.setUserId(UserHolder.getUser().getId());
        //6.3 优惠券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
