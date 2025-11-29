package com.tala.reminder.service;

import com.tala.core.exception.ErrorCode;
import com.tala.core.exception.TalaException;
import com.tala.reminder.domain.Reminder;
import com.tala.reminder.dto.CreateReminderRequest;
import com.tala.reminder.dto.ReminderResponse;
import com.tala.reminder.dto.UpdateReminderRequest;
import com.tala.reminder.mapper.ReminderMapper;
import com.tala.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Reminder service for CRUD operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {
    
    private final ReminderRepository repository;
    private final ReminderMapper mapper;
    
    /**
     * Create new reminder
     */
    @Transactional
    public ReminderResponse create(CreateReminderRequest request) {
        log.info("Creating reminder: category={}, userId={}", 
            request.getCategory(), request.getUserId());
        
        Reminder reminder = mapper.toEntity(request);
        Reminder saved = repository.save(reminder);
        
        log.info("Reminder created: id={}, dueAt={}", saved.getId(), saved.getDueAt());
        
        return mapper.toResponse(saved);
    }
    
    /**
     * Get reminder by ID
     */
    @Transactional(readOnly = true)
    public ReminderResponse getById(Long id) {
        Reminder reminder = repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new TalaException(
                ErrorCode.NOT_FOUND, 
                "Reminder not found: " + id
            ));
        
        return mapper.toResponse(reminder);
    }
    
    /**
     * Update reminder
     */
    @Transactional
    public ReminderResponse update(Long id, UpdateReminderRequest request) {
        Reminder reminder = repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new TalaException(
                ErrorCode.NOT_FOUND, 
                "Reminder not found: " + id
            ));
        
        mapper.updateEntity(request, reminder);
        Reminder updated = repository.save(reminder);
        
        return mapper.toResponse(updated);
    }
    
    /**
     * Delete reminder (soft delete)
     */
    @Transactional
    public void delete(Long id) {
        Reminder reminder = repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new TalaException(
                ErrorCode.NOT_FOUND, 
                "Reminder not found: " + id
            ));
        
        reminder.softDelete();
        repository.save(reminder);
        
        log.info("Reminder soft deleted: id={}", id);
    }
    
    /**
     * Complete reminder
     */
    @Transactional
    public ReminderResponse complete(Long id) {
        Reminder reminder = repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new TalaException(
                ErrorCode.NOT_FOUND, 
                "Reminder not found: " + id
            ));
        
        reminder.complete();
        Reminder updated = repository.save(reminder);
        
        log.info("Reminder completed: id={}", id);
        
        return mapper.toResponse(updated);
    }
    
    /**
     * Snooze reminder
     */
    @Transactional
    public ReminderResponse snooze(Long id, Instant until) {
        Reminder reminder = repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new TalaException(
                ErrorCode.NOT_FOUND, 
                "Reminder not found: " + id
            ));
        
        reminder.snooze(until);
        Reminder updated = repository.save(reminder);
        
        log.info("Reminder snoozed: id={}, until={}", id, until);
        
        return mapper.toResponse(updated);
    }
    
    /**
     * Cancel reminder
     */
    @Transactional
    public ReminderResponse cancel(Long id) {
        Reminder reminder = repository.findByIdAndNotDeleted(id)
            .orElseThrow(() -> new TalaException(
                ErrorCode.NOT_FOUND, 
                "Reminder not found: " + id
            ));
        
        reminder.cancel();
        Reminder updated = repository.save(reminder);
        
        log.info("Reminder canceled: id={}", id);
        
        return mapper.toResponse(updated);
    }
    
    /**
     * Get active reminders for user
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getActiveReminders(Long userId, Long profileId) {
        List<Reminder> reminders = repository.findActiveByUserAndProfile(userId, profileId);
        return mapper.toResponseList(reminders);
    }
    
    /**
     * Get due reminders
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getDueReminders(Long userId, Instant startTime, Instant endTime) {
        List<Reminder> reminders = repository.findDueReminders(
            userId, startTime, endTime, Instant.now()
        );
        return mapper.toResponseList(reminders);
    }
    
    /**
     * Get reminders by category
     */
    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersByCategory(Long userId, String category) {
        List<Reminder> reminders = repository.findByCategory(userId, category);
        return mapper.toResponseList(reminders);
    }
    
    /**
     * Count active reminders
     */
    @Transactional(readOnly = true)
    public long countActiveReminders(Long userId) {
        return repository.countActiveByUser(userId);
    }
}
