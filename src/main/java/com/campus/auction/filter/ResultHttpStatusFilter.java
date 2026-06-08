package com.campus.auction.filter;

import com.campus.auction.common.Result;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

/**
 * JAX-RS replacement for Spring MVC's ResultHttpStatusAdvice.
 *
 * When a resource method returns Result&lt;T&gt; directly, this filter reads
 * Result.code() and syncs it to the HTTP response status line — so callers
 * see 201 for Result.created(), 200 for Result.ok(), etc.
 *
 * This runs at the default USER priority (5000), which in response-filter ordering
 * (descending) means it executes before JwtSecurityFilter (priority 1000).
 */
@Provider
@Component
public class ResultHttpStatusFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object entity = responseContext.getEntity();
        if (entity instanceof Result<?> result) {
            responseContext.setStatus(result.code());
        }
    }
}
