package com.tala.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Client for user-service
 * Fetches baby profile data for AI context enrichment
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    
    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;
    
    private final ObjectMapper objectMapper;
    
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    
    /**
     * Get baby profile by ID
     */
    public ProfileData getProfile(Long profileId) throws IOException {
        String url = userServiceUrl + "/api/v1/profiles/" + profileId;
        
        log.debug("Fetching profile from user-service: {}", url);
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("User-service API error: " + response.code() + ", body: " + errorBody);
            }
            
            String responseBody = response.body().string();
            log.debug("Profile fetched successfully from user-service");
            return objectMapper.readValue(responseBody, ProfileData.class);
        }
    }
    
    /**
     * Health check for user-service
     */
    public boolean isHealthy() {
        String url = userServiceUrl + "/actuator/health";
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            log.warn("User-service health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Profile data DTO for deserialization
     */
    public static class ProfileData {
        public Long id;
        public Long userId;
        public String babyName;
        public LocalDate birthDate;
        public String timezone;
        public String gender;
        public String photoUrl;
        public String parentName;
        public String parentRole;
        public String zipcode;
        public String concerns;
        public Boolean hasDaycare;
        public String daycareName;
        public String updateMethod;
        public Integer ageInDays;
        public Boolean isDefault;
    }
}
