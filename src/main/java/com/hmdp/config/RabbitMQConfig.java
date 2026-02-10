package com.hmdp.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // 订单处理队列
    public static final String VOUCHER_ORDER_QUEUE = "voucher.order.queue";
    
    @Bean
    public Queue voucherOrderQueue() {
        return new Queue(VOUCHER_ORDER_QUEUE, true); // durable=true 持久化队列
    }
    
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}