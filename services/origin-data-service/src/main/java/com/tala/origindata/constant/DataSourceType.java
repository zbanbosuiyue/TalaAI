package com.tala.origindata.constant;

/**
 * Data source types for origin data
 * Supports both event-based and document-based data types
 */
public enum DataSourceType {
    // Event-based sources
    DAY_CARE_REPORT,
    INCIDENT_REPORT,
    HEALTH_REPORT,
    HOME_EVENT,
    AI_CHAT,  // User chat input processed by AI service
    
    // Document-based sources (non-event)
    WEEKLY_CURRICULUM,
    DAILY_CURRICULUM
}
