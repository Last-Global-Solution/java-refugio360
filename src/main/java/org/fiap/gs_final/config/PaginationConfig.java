package org.fiap.gs_final.config;

import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaginationConfig {
    @Bean PageableHandlerMethodArgumentResolverCustomizer paginationLimit() {
        return resolver -> resolver.setMaxPageSize(100);
    }
}
