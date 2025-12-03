package com.tala.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Error Controller
 * 
 * Handles /error endpoint to prevent AccessDeniedException
 * after SSE stream completion.
 */
@RestController
@Slf4j
public class CustomErrorController implements ErrorController {
    
    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        // Get error status
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        
        // If it's from SSE completion, just return empty response
        if (status == null) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
        
        Integer statusCode = Integer.valueOf(status.toString());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", statusCode);
        errorResponse.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        
        return ResponseEntity.status(statusCode).body(errorResponse);
    }
}
