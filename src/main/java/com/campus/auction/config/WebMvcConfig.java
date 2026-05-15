package com.campus.auction.config;

import com.campus.auction.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                // Public endpoints that never require a token
                .excludePathPatterns(
                        "/users/login",
                        "/users/register",
                        "/api/images/**"   // served as static files — no auth required to view
                );
    }

    /**
     * Serve uploaded images as static resources.
     *
     * <p>Requests to {@code GET /api/images/{filename}} are resolved directly from the
     * local filesystem at {@code uploadDir}/{filename} — no controller method involved.
     * The JwtInterceptor also excludes this path, so images are publicly readable.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/images/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
