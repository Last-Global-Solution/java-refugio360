package org.fiap.gs_final.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${GEMINI_API_KEY:}' != ''")
public class SpringAiConfig {
    @Bean ChatClient chatClient(ChatClient.Builder builder) { return builder.build(); }
}
