package com.celcioblond.pms.config;

import com.celcioblond.pms.security.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.rate-limit.ai.requests-per-minute}")
    private int aiRequestsPerMinute;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(aiRequestsPerMinute, Clock.systemUTC()))
                .addPathPatterns("/api/chat/**", "/api/project/generate-*");
    }
}
