package com.tala.personalization.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for Reminder Service
 */
@FeignClient(name = "reminder-service", url = "${feign.services.reminder-service.url}")
public interface ReminderServiceClient {
    
    @GetMapping("/api/v1/reminders/due")
    List<ReminderResponse> getDueReminders(
        @RequestParam Long userId,
        @RequestParam Long profileId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
    
    @GetMapping("/api/v1/reminders")
    List<ReminderResponse> getActiveReminders(
        @RequestParam Long userId,
        @RequestParam Long profileId
    );
    
    class ReminderResponse {
        public Long id;
        public String title;
        public String description;
        public String category;
        public LocalDate dueAt;
        public String priority;
        public String status;
        public Boolean canSnooze;
    }
}
