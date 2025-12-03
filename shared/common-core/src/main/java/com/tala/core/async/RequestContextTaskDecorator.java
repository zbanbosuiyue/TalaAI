package com.tala.core.async;

import com.tala.core.security.JwtContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Task decorator that propagates request context and JWT token to async threads
 * 
 * This ensures that:
 * 1. Spring's RequestContextHolder is available in async tasks
 * 2. JWT token is propagated to async threads for downstream service calls
 * 
 * Industry Best Practice:
 * - Capture context from parent thread before async execution
 * - Set context in async thread before task execution
 * - Clean up context after task completion to prevent memory leaks
 */
@Slf4j
public class RequestContextTaskDecorator implements TaskDecorator {
    
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capture context from current thread
        RequestAttributes context = RequestContextHolder.getRequestAttributes();
        String jwtToken = JwtContextHolder.getToken();
        
        return () -> {
            try {
                // Set context in async thread
                if (context != null) {
                    RequestContextHolder.setRequestAttributes(context);
                }
                if (jwtToken != null) {
                    JwtContextHolder.setToken(jwtToken);
                }
                
                // Execute the task
                runnable.run();
            } finally {
                // Clean up context to prevent memory leaks
                RequestContextHolder.resetRequestAttributes();
                JwtContextHolder.clear();
            }
        };
    }
}
