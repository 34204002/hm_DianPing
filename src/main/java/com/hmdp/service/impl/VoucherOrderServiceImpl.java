package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.rabbitmq.client.Channel;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
@EnableAsync
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // Lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    
    // RabbitMQ 队列名称
    private static final String ORDER_QUEUE = "voucher.order.queue";
    
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/sekill.lua")));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    
    /**
     * RabbitMQ 消费者 - 处理订单
     * 手动确认模式下，需要显式确认消息
     */
    @RabbitListener(queues = ORDER_QUEUE)
    public void handleVoucherOrder(VoucherOrder order, Channel channel) {
        try {
            log.info("开始处理订单: orderId={}, voucherId={}", order.getId(), order.getVoucherId());
            
            // 保存订单到数据库
            save(order);
            log.info("订单保存成功: orderId={}", order.getId());
            
            // 更新数据库中的库存字段
            boolean updateSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", order.getVoucherId())
                .update();
            
            if (updateSuccess) {
                log.info("库存更新成功: voucherId={}, 剩余库存={}", 
                    order.getVoucherId(), 
                    getRemainingStock(order.getVoucherId()));
            } else {
                log.warn("库存更新可能失败: voucherId={}", order.getVoucherId());
            }
            
        } catch (Exception e) {
            log.error("处理订单失败: orderId={}, errorMessage={}", order.getId(), e.getMessage(), e);
            // 在手动确认模式下，不确认消息会让消息重新入队
            throw e; // 重新抛出异常让RabbitMQ处理重试
        }
    }
    
    /**
     * 获取剩余库存（用于日志显示）
     */
    private Integer getRemainingStock(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        return voucher != null ? voucher.getStock() : 0;
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.从Redis读取时间信息
        String timeKey = RedisConstants.SECKILL_VOUCHER_TIME_KEY + voucherId;
        Set<String> timeSet = stringRedisTemplate.opsForSet().members(timeKey);
        
        if (timeSet == null || timeSet.isEmpty()) {
            return Result.fail("优惠券时间信息不存在");
        }
        
        //2.解析时间信息
        LocalDateTime beginTime = null;
        LocalDateTime endTime = null;
        
        for (String member : timeSet) {
            if (member.startsWith("beginTime:")) {
                beginTime = LocalDateTime.parse(member.substring(10));
            } else if (member.startsWith("endTime:")) {
                endTime = LocalDateTime.parse(member.substring(8));
            }
        }
        
        if (beginTime == null || endTime == null) {
            return Result.fail("优惠券时间信息不完整");
        }
        
        //3.判断秒杀是否开始
        if (beginTime.isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        //4.判断秒杀是否结束
        if (endTime.isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        
        //4.执行Lua脚本进行秒杀
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
            SECKILL_SCRIPT,
            Arrays.asList(RedisConstants.SECKILL_STOCK_KEY + voucherId, RedisConstants.SECKILL_VOUCHER_USER_KEY + voucherId),
            voucherId.toString(),
            userId.toString()
        );
        
        //5.根据Lua脚本返回结果处理
        if (result == 1) {
            return Result.fail("库存不足");
        } else if (result == 2) {
            return Result.fail("用户已下单");
        }
        
        //6.创建订单对象并放入队列
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId("order"));
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setStatus(1);
        voucherOrder.setCreateTime(LocalDateTime.now());
        
        try {
            // 发送到 RabbitMQ 队列
            rabbitTemplate.convertAndSend(ORDER_QUEUE, voucherOrder);
        } catch (Exception e) {
            return Result.fail("下单失败：" + e.getMessage());
        }
        
        //7.立即返回成功给用户
        return Result.ok("下单成功，订单正在处理中");
    }
}
