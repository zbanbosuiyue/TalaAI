package com.tala.query.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for origin-data-service
 * Used by query-service to fetch timeline data for aggregation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OriginDataServiceClient {
    
    @Value("${services.origin-data-service.url:http://localhost:8089}")
    private String originDataServiceUrl;
    
    private final ObjectMapper objectMapper;
    
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    
    /**
     * Get timeline entries for a profile within a date range
     */
    public List<TimelineEntryData> getTimelineByDateRange(Long profileId, LocalDate date) throws IOException {
        // Convert date to time range (start of day to end of day)
        Instant startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        String url = String.format(
            "%s/api/v1/timeline/profile/%d/range?startTime=%s&endTime=%s",
            originDataServiceUrl,
            profileId,
            startTime.toString(),
            endTime.toString()
        );
        
        log.debug("Fetching timeline from origin-data-service: {}", url);
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("Origin-data-service API error: " + response.code() + ", body: " + errorBody);
            }
            
            String responseBody = response.body().string();
            log.debug("Timeline fetched successfully from origin-data-service");
            
            // Parse response as list of timeline entries
            return objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, TimelineEntryData.class)
            );
        }
    }
    
    /**
     * Health check for origin-data-service
     */
    public boolean isHealthy() {
        String url = originDataServiceUrl + "/api/v1/health";
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            log.warn("Origin-data-service health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Timeline entry data DTO for deserialization
     */
    public static class TimelineEntryData {
        public Long id;
        public Long profileId;
        public String timelineType;
        public String dataSource;
        public String recordTime;
        public String title;
        public String aiSummary;
        public Object aiTags;
        public String attachmentUrls;
        public String location;
        public String aiModelVersion;
        public String createdAt;
        public String updatedAt;
    }
}
