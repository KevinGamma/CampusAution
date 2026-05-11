package com.campus.auction.context;

import com.campus.auction.enums.UserRole;

/**
 * Thread-local store for the authenticated user's identity.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link com.campus.auction.interceptor.JwtInterceptor#preHandle} calls {@link #set}
 *       after a valid JWT is verified.</li>
 *   <li>Service methods call {@link #get} to read the current user.</li>
 *   <li>{@link com.campus.auction.interceptor.JwtInterceptor#afterCompletion} calls
 *       {@link #clear} to prevent ThreadLocal leaks between pooled threads.</li>
 * </ol>
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(Long userId, String username, UserRole role) {
        HOLDER.set(new CurrentUser(userId, username, role));
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** Immutable snapshot of the JWT claims needed by the service layer. */
    public record CurrentUser(Long userId, String username, UserRole role) {}
}
