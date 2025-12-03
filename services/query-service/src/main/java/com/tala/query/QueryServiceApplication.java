package com.tala.query;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Query Service Application
 * 
 * Analytics and insights from ClickHouse
 */
@SpringBootApplication(scanBasePackages = {"com.tala.query", "com.tala.core"})
@EnableKafka
@EnableCaching
@EnableFeignClients(basePackages = "com.tala.query.client")
public class QueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryServiceApplication.class, args);
    }
}
