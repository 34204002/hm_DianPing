package com.hmdp.config;

import com.hmdp.utils.ShopSearchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient searchChatClient(ChatModel chatModel, ShopSearchTools tools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(new ClassPathResource("ai/system-prompt.md"), StandardCharsets.UTF_8)
                .defaultTools(tools)
                .build();
    }
}
