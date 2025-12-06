package com.tala.core.config;

import com.tala.core.async.RequestContextTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Base Async Configuration for Tala microservices
 * 
 * Provides common async executor setup with request context propagation.
 * This ensures JWT tokens and user context are available in async threads.
 * 
 * Usage:
 * <pre>
 * @Configuration
 * @EnableAsync
 * public class MyServiceAsyncConfig {
 *     
 *     @Bean(name = "asyncExecutor")
 *     public Executor asyncExecutor() {
 *         return BaseAsyncConfig.createAsyncExecutor(10, 20, 100, "my-service-async-");
 *     }
 * }
 * </pre>
 * 
 * Default pool sizes:
 * - Core: 10 threads
 * - Max: 20 threads
 * - Queue: 100 tasks
 * 
 * Adjust based on service load:
 * - High traffic services (ai-service, gateway): Larger pools
 * - Low traffic services (reminder-service): Smaller pools
 */
public class BaseAsyncConfig {
    
    /**
     * Create async executor with request context propagation
     * 
     * @param corePoolSize Core number of threads
     * @param maxPoolSize Maximum number of threads
     * @param queueCapacity Queue capacity for pending tasks
     * @param threadNamePrefix Prefix for thread names (e.g., "ai-async-")
     * @return Configured executor
     */
    public static Executor createAsyncExecutor(
            int corePoolSize,
            int maxPoolSize,
            int queueCapacity,
            String threadNamePrefix
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(new RequestContextTaskDecorator());
        executor.initialize();
        return executor;
    }
    
    /**
     * Create async executor with default settings
     * Core: 10, Max: 20, Queue: 100
     */
    public static Executor createAsyncExecutor(String threadNamePrefix) {
        return createAsyncExecutor(10, 20, 100, threadNamePrefix);
    }
}
