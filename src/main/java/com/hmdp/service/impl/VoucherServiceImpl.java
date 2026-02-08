package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        // 对于历史查询需求，时间信息永久保存（不设置过期时间）
        // 库存信息设置合理过期时间以节省内存
        long durationMinutes = java.time.Duration.between(voucher.getBeginTime(), voucher.getEndTime()).toMinutes();
        long stockExpireMinutes = durationMinutes + 30; // 库存信息：活动时长 + 30分钟缓冲
        
        // 保存库存信息到redis（带过期时间）
        stringRedisTemplate.opsForValue().set(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
        stringRedisTemplate.expire(RedisConstants.SECKILL_STOCK_KEY + voucher.getId(), stockExpireMinutes, java.util.concurrent.TimeUnit.MINUTES);
        
        // 单独存储时间信息到Set中（永久保存，用于历史查询）
        String timeKey = RedisConstants.SECKILL_VOUCHER_TIME_KEY + voucher.getId();
        stringRedisTemplate.opsForSet().add(timeKey, "beginTime:" + voucher.getBeginTime().toString());
        stringRedisTemplate.opsForSet().add(timeKey, "endTime:" + voucher.getEndTime().toString());
        // 时间信息不设置过期时间，永久保存供历史查询
    }

    @Override
    public void addVoucher(Voucher voucher) {
        save(voucher);
    }
}