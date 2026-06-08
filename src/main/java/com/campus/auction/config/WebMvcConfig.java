package com.campus.auction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Residual Spring MVC configuration after the Jersey migration.
 *
 * <p>JWT authentication is now handled by
 * {@link com.campus.auction.filter.JwtSecurityFilter} (a JAX-RS
 * {@code ContainerRequestFilter}), so the old {@code JwtInterceptor} registration
 * has been removed. The only remaining Spring MVC responsibility is serving uploaded
 * images as static files — requests that Jersey (running as a servlet filter) does not
 * match fall through to the {@code DispatcherServlet} and are resolved here.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    /**
     * Serve uploaded images as static resources.
     * Requests to {@code GET /api/v1/images/{filename}} are resolved directly from the
     * local filesystem at {@code uploadDir}/{filename} — no controller method involved.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/images/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
