package com.tala.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Global Jackson Configuration for all Tala services
 * 
 * Serializes Long IDs as strings to prevent JavaScript precision loss.
 * 
 * JavaScript safe integer range: -(2^53 - 1) to (2^53 - 1) = ±9,007,199,254,740,991
 * Java Long range: -2^63 to 2^63 - 1 = ±9,223,372,036,854,775,807
 * 
 * Snowflake IDs (used by IdGenerator) are 64-bit and exceed JavaScript's safe range.
 * Without this config, frontend receives corrupted IDs like:
 *   Backend: 2533450878446929992
 *   Frontend: 2533450878446930000 (precision lost)
 * 
 * Industry Best Practice:
 * - Twitter API returns IDs as both number and string ("id" and "id_str")
 * - GitHub API returns all IDs as strings
 * - We follow GitHub's approach: all Long IDs serialized as strings
 */
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // Serialize all Long values as strings to prevent precision loss in JavaScript
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        
        objectMapper.registerModule(module);
        
        return objectMapper;
    }
}
