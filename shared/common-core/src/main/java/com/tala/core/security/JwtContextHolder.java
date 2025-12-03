package com.tala.core.security;

/**
 * Thread-local holder for JWT token
 * 
 * Used to pass JWT tokens to async threads where RequestContextHolder
 * is not available.
 */
public final class JwtContextHolder {
    
    private static final ThreadLocal<String> jwtToken = new ThreadLocal<>();
    
    private JwtContextHolder() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Set JWT token for current thread
     */
    public static void setToken(String token) {
        jwtToken.set(token);
    }
    
    /**
     * Get JWT token for current thread
     */
    public static String getToken() {
        return jwtToken.get();
    }
    
    /**
     * Clear JWT token for current thread
     */
    public static void clear() {
        jwtToken.remove();
    }
}
