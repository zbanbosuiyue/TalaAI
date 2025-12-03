package com.tala.ai.config;

import com.tala.core.async.RequestContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for AI Service
 * 
 * Configures async task execution with request context propagation
 * to ensure JWT tokens are available in async threads.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Configure async executor with request context propagation
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new RequestContextTaskDecorator());
        executor.initialize();
        return executor;
    }
}
