package com.tala.core.feign;

import com.tala.core.exception.ErrorCode;
import com.tala.core.exception.TalaException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Custom Feign Error Decoder for handling downstream service errors
 * 
 * Converts HTTP error responses from downstream services into appropriate exceptions
 * with proper error codes and messages.
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        String serviceName = extractServiceName(methodKey);
        int status = response.status();
        String errorBody = extractErrorBody(response);
        
        log.error("Feign client error from service: {}, status: {}, method: {}, body: {}", 
                serviceName, status, methodKey, errorBody);
        
        // Map HTTP status codes to appropriate exceptions
        return switch (status) {
            case 400 -> new TalaException(ErrorCode.BAD_REQUEST, 
                    String.format("Bad request to %s: %s", serviceName, errorBody));
            case 401 -> new TalaException(ErrorCode.UNAUTHORIZED, 
                    String.format("Unauthorized access to %s: %s", serviceName, errorBody));
            case 403 -> new TalaException(ErrorCode.FORBIDDEN, 
                    String.format("Forbidden access to %s: %s", serviceName, errorBody));
            case 404 -> new TalaException(ErrorCode.NOT_FOUND, 
                    String.format("Resource not found in %s: %s", serviceName, errorBody));
            case 409 -> new TalaException(ErrorCode.CONFLICT, 
                    String.format("Conflict in %s: %s", serviceName, errorBody));
            case 422 -> new TalaException(ErrorCode.VALIDATION_ERROR, 
                    String.format("Validation error in %s: %s", serviceName, errorBody));
            case 500, 502, 503, 504 -> new TalaException(ErrorCode.INTERNAL_ERROR, 
                    String.format("Service %s error: %s", serviceName, errorBody));
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
    
    /**
     * Extract service name from Feign method key
     */
    private String extractServiceName(String methodKey) {
        if (methodKey == null || methodKey.isEmpty()) {
            return "unknown-service";
        }
        // Method key format: ClassName#methodName(params)
        String[] parts = methodKey.split("#");
        if (parts.length > 0) {
            String className = parts[0];
            int lastDot = className.lastIndexOf('.');
            return lastDot > 0 ? className.substring(lastDot + 1) : className;
        }
        return methodKey;
    }
    
    /**
     * Extract error body from response
     */
    private String extractErrorBody(Response response) {
        try {
            if (response.body() != null) {
                byte[] bodyData = response.body().asInputStream().readAllBytes();
                return new String(bodyData, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Failed to read error response body", e);
        }
        return "No error body available";
    }
}
