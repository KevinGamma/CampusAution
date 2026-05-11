package com.campus.auction.interceptor;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.context.UserContext;
import com.campus.auction.enums.UserRole;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Per-request JWT and role guard.
 *
 * <p>Flow for a protected endpoint (method annotated with {@link RoleAccess}):
 * <ol>
 *   <li>Extract {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validate signature and expiry via {@link JwtUtils#parseToken}.</li>
 *   <li>Verify the caller's role is in the annotation's allowed set.</li>
 *   <li>Populate {@link UserContext} so the service layer can read the caller's identity.</li>
 * </ol>
 *
 * <p>Unannotated endpoints (e.g. {@code GET /auctions}, {@code POST /users/login}) are
 * passed through without any token check.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // Only inspect Spring MVC handler methods; let static resources pass.
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        RoleAccess roleAccess = hm.getMethodAnnotation(RoleAccess.class);
        if (roleAccess == null) {
            // Endpoint is public — no token required.
            return true;
        }

        // ── 1. Extract Bearer token ───────────────────────────────────────────
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ServiceException(HttpStatus.UNAUTHORIZED,
                    "Missing or malformed Authorization header (expected: Bearer <token>)");
        }
        String token = authHeader.substring(7).trim();

        // ── 2. Validate JWT ───────────────────────────────────────────────────
        Claims claims;
        try {
            claims = jwtUtils.parseToken(token);
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            throw new ServiceException(HttpStatus.UNAUTHORIZED,
                    "Token is invalid or has expired — please log in again");
        }

        // ── 3. Check role ─────────────────────────────────────────────────────
        UserRole callerRole = UserRole.valueOf(claims.get("role", String.class));
        boolean permitted = Arrays.asList(roleAccess.value()).contains(callerRole);
        if (!permitted) {
            throw new ServiceException(HttpStatus.FORBIDDEN,
                    "Access denied — required role(s): " + Arrays.toString(roleAccess.value()));
        }

        // ── 4. Populate UserContext for the service layer ─────────────────────
        Long   userId   = claims.get("userId",   Integer.class).longValue();
        String username = claims.get("username", String.class);
        UserContext.set(userId, username, callerRole);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // Always clear the ThreadLocal to prevent data leaking across pooled threads.
        UserContext.clear();
    }
}
