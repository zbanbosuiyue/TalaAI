package com.tala.core.feign;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base Feign Configuration with JWT Token Propagation
 * 
 * This configuration can be imported by any service that needs to make
 * authenticated calls to other microservices.
 * 
 * Usage in service:
 * @Import(FeignJwtConfig.class)
 * or
 * @FeignClient(name = "service-name", configuration = FeignJwtConfig.class)
 */
@Configuration
public class FeignJwtConfig {
    
    /**
     * JWT token propagation interceptor
     * Automatically forwards JWT tokens from incoming requests to downstream services
     */
    @Bean
    public RequestInterceptor jwtFeignRequestInterceptor() {
        return new JwtFeignRequestInterceptor();
    }
    
    /**
     * Feign logger level for debugging
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    
    /**
     * Custom error decoder for better error handling
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
