package com.campus.auction.filter;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.context.UserContext;
import com.campus.auction.enums.UserRole;
import com.campus.auction.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * JAX-RS replacement for Spring MVC's JwtInterceptor.
 *
 * Request phase  — reads @RoleAccess from the matched resource method via ResourceInfo,
 *                  validates the Bearer token, checks role, and populates UserContext.
 *                  Aborts with 401/403 + Result.fail(...) JSON on any failure.
 *
 * Response phase — always clears the UserContext ThreadLocal (mirrors
 *                  JwtInterceptor.afterCompletion) to prevent cross-request leakage
 *                  in the servlet thread pool.
 */
@Provider
@Component
@Priority(Priorities.AUTHENTICATION)
@RequiredArgsConstructor
public class JwtSecurityFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Jersey injects a per-request proxy here even though the filter is a singleton bean.
    // The jersey-spring6 bridge ensures @Context injections are processed on Spring beans.
    @Context
    private ResourceInfo resourceInfo;

    private final JwtUtils jwtUtils;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RoleAccess roleAccess = resourceInfo.getResourceMethod() != null
                ? resourceInfo.getResourceMethod().getAnnotation(RoleAccess.class)
                : null;

        if (roleAccess == null) {
            return; // public endpoint — no token required
        }

        // ── 1. Extract Bearer token ───────────────────────────────────────────
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abort(requestContext, Response.Status.UNAUTHORIZED,
                    "Missing or malformed Authorization header (expected: Bearer <token>)");
            return;
        }
        String token = authHeader.substring(7).trim();

        // ── 2. Validate JWT ───────────────────────────────────────────────────
        Claims claims;
        try {
            claims = jwtUtils.parseToken(token);
        } catch (Exception ex) {
            abort(requestContext, Response.Status.UNAUTHORIZED,
                    "Token is invalid or has expired — please log in again");
            return;
        }

        // ── 3. Check role ─────────────────────────────────────────────────────
        UserRole callerRole = UserRole.valueOf(claims.get("role", String.class));
        if (!Arrays.asList(roleAccess.value()).contains(callerRole)) {
            abort(requestContext, Response.Status.FORBIDDEN,
                    "Access denied — required role(s): " + Arrays.toString(roleAccess.value()));
            return;
        }

        // ── 4. Populate UserContext for the service layer ─────────────────────
        Long   userId   = claims.get("userId",   Integer.class).longValue();
        String username = claims.get("username", String.class);
        UserContext.set(userId, username, callerRole);
    }

    /** Always runs (even after abortWith) — clears the ThreadLocal before the thread is reused. */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        UserContext.clear();
    }

    private static void abort(ContainerRequestContext ctx, Response.Status status, String message) {
        ctx.abortWith(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Result.fail(status.getStatusCode(), message))
                .build());
    }
}
