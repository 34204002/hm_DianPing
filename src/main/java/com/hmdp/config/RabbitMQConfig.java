package com.hmdp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // 订单处理队列
    public static final String VOUCHER_ORDER_QUEUE = "voucher.order.queue";
    
    /**
     * 简单的订单处理队列
     */
    @Bean
    public Queue voucherOrderQueue() {
        return new Queue(VOUCHER_ORDER_QUEUE, true); // durable=true 持久化队列
    }
    
    /**
     * 配置支持 Java 8 时间类型的 ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
    
    /**
     * 配置消息转换器，支持 LocalDateTime 等时间类型
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setCreateMessageIds(true);
        return converter;
    }
    
    /**
     * 配置 RabbitTemplate，使用自定义的消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        // 设置默认交换机和路由键
        template.setExchange("");
        template.setRoutingKey(VOUCHER_ORDER_QUEUE);
        return template;
    }
}