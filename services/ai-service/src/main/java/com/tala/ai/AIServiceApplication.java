package com.tala.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Service Application
 * 
 * Pattern detection, insights generation, and AI-powered analytics
 */
@SpringBootApplication(scanBasePackages = {"com.tala.ai", "com.tala.core"})
@EnableKafka
@EnableScheduling
@EnableFeignClients(basePackages = "com.tala.ai.client")
public class AIServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIServiceApplication.class, args);
    }
}
