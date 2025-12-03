package com.tala.personalization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Personalization Service - Intelligent content orchestration
 * 
 * Core responsibilities:
 * - Today Menu content generation and orchestration
 * - Insights page personalization
 * - Tala conversation starters generation
 * - Priority and urgency calculation
 * - Multi-service data aggregation
 */
@SpringBootApplication(scanBasePackages = {"com.tala.personalization", "com.tala.core"})
@EnableFeignClients
@EnableCaching
@EnableAsync
public class PersonalizationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PersonalizationServiceApplication.class, args);
    }
}
