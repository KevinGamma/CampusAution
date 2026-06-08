package com.campus.auction.config;

import com.campus.auction.filter.GeneralExceptionMapper;
import com.campus.auction.filter.JwtSecurityFilter;
import com.campus.auction.filter.LocalDateParamConverter;
import com.campus.auction.filter.ResultHttpStatusFilter;
import com.campus.auction.filter.ServiceExceptionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import jakarta.inject.Inject;

/**
 * Central Jersey (JAX-RS) configuration.
 *
 * Jersey runs alongside Spring MVC (spring.jersey.type=filter in application.yml).
 * Requests matched by a @Path resource are handled here; everything else
 * (e.g. GET /api/v1/images/** static files) falls through to Spring MVC.
 *
 * Registration order matters for request filters (ascending priority = runs first):
 *   1. JwtSecurityFilter  (priority 1000 = AUTHENTICATION) — auth + UserContext
 *   2. ResultHttpStatusFilter (default 5000 = USER, runs first in response phase)
 */
@Configuration
public class JerseyConfig extends ResourceConfig {

    @Inject
    public JerseyConfig(ObjectMapper objectMapper) {

        // ── Jackson ───────────────────────────────────────────────────────────
        // Reuse Spring's auto-configured ObjectMapper so Jersey honours all
        // spring.jackson.* settings (date-format, time-zone, etc.) from application.yml.
        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper(objectMapper);
        register(jacksonProvider);

        // ── Features ──────────────────────────────────────────────────────────
        register(MultiPartFeature.class);           // multipart/form-data (file uploads)

        // ── Filters ───────────────────────────────────────────────────────────
        register(JwtSecurityFilter.class);          // JWT auth + role guard + UserContext cleanup
        register(ResultHttpStatusFilter.class);     // sync HTTP status from Result.code

        // ── Exception mappers ────────────────────────────────────────────────
        register(ServiceExceptionMapper.class);     // ServiceException → 4xx/5xx JSON
        register(GeneralExceptionMapper.class);     // any other Exception → 500 JSON

        // ── ParamConverters ──────────────────────────────────────────────────
        register(LocalDateParamConverter.class);    // @QueryParam LocalDate (yyyy-MM-dd)

        // ── Resource classes ─────────────────────────────────────────────────
        // Auto-scans com.campus.auction.resource for all @Path-annotated classes.
        packages("com.campus.auction.resource");
    }
}
