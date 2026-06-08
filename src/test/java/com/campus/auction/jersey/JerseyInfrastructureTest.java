package com.campus.auction.jersey;

import com.campus.auction.annotation.RoleAccess;
import com.campus.auction.common.Result;
import com.campus.auction.context.UserContext;
import com.campus.auction.dto.UserDTO;
import com.campus.auction.enums.UserRole;
import com.campus.auction.exception.ServiceException;
import com.campus.auction.filter.GeneralExceptionMapper;
import com.campus.auction.filter.JwtSecurityFilter;
import com.campus.auction.filter.ResultHttpStatusFilter;
import com.campus.auction.filter.ServiceExceptionMapper;
import com.campus.auction.utils.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Jersey presentation-layer infrastructure, run against a real
 * Grizzly2 HTTP server on a random port via the Jersey Test Framework.
 *
 * <p>Deliberately Spring-free and database-free: it wires the real {@link JwtSecurityFilter},
 * {@link ResultHttpStatusFilter}, {@link ServiceExceptionMapper} and {@link GeneralExceptionMapper}
 * around a synthetic {@link ProbeResource} so the cross-cutting behaviour can be verified in
 * isolation from the MyBatis/MySQL service stack.
 *
 * <p>Covers the three required scenarios:
 * <ol>
 *   <li>{@code @RoleAccess} enforcement by {@link JwtSecurityFilter} — 401 / 403 with the
 *       standard {@code Result} JSON payload, plus strict {@link UserContext} cleanup.</li>
 *   <li>{@link ResultHttpStatusFilter} mapping {@code Result.code()} onto the HTTP status line
 *       (e.g. a 201 business code → HTTP 201 Created).</li>
 *   <li>Business and generic exceptions translated by the custom exception mappers.</li>
 * </ol>
 */
class JerseyInfrastructureTest extends JerseyTest {

    private static final String SECRET = "campus-auction-jwt-secret-key-for-hs256-must-be-long-enough-32chars";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Shared JwtUtils, hand-wired (no Spring) with a known secret/expiry. */
    private static final JwtUtils JWT_UTILS = new JwtUtils();

    @Override
    protected Application configure() {
        ReflectionTestUtils.setField(JWT_UTILS, "secret", SECRET);
        ReflectionTestUtils.setField(JWT_UTILS, "expirationMs", 3_600_000L); // 1 hour

        return new ResourceConfig()
                .register(new JwtSecurityFilter(JWT_UTILS))   // real auth filter (instance: carries JwtUtils)
                .register(ResultHttpStatusFilter.class)       // real status-sync filter
                .register(ServiceExceptionMapper.class)       // real business-exception mapper
                .register(GeneralExceptionMapper.class)       // real catch-all mapper
                .register(ContextProbeFilter.class)           // test probe: observes UserContext AFTER cleanup
                .register(ProbeResource.class)                // synthetic resource exercising the above
                .register(JacksonFeature.class);              // JSON (de)serialization of Result
    }

    @Override
    protected void configureClient(org.glassfish.jersey.client.ClientConfig config) {
        config.register(JacksonFeature.class);
    }

    @BeforeEach
    void resetProbe() {
        ContextProbeFilter.OBSERVED_AFTER_CLEANUP.set("UNSET");
    }

    // Helpers ────────────────────────────────────────────────────────────────

    private String tokenFor(long id, String username, UserRole role) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setUsername(username);
        dto.setRole(role);
        return JWT_UTILS.generateToken(dto);
    }

    private Response get(String path, String bearer) {
        var builder = target(path).request(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            builder = builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        }
        return builder.get();
    }

    private JsonNode body(Response response) throws Exception {
        return JSON.readTree(response.readEntity(String.class));
    }

    // ── Scenario 1: @RoleAccess enforcement ───────────────────────────────────

    @Test
    @DisplayName("Public endpoint (no @RoleAccess) is reachable without a token")
    void publicEndpoint_noToken_ok() throws Exception {
        Response resp = get("/probe/public", null);
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(body(resp).get("data").asText()).isEqualTo("anonymous");
    }

    @Test
    @DisplayName("Protected endpoint without a token → 401 with standard Result payload")
    void protectedEndpoint_missingToken_401() throws Exception {
        Response resp = get("/probe/secure", null);
        assertThat(resp.getStatus()).isEqualTo(401);

        JsonNode json = body(resp);
        assertThat(json.get("code").asInt()).isEqualTo(401);
        assertThat(json.get("msg").asText()).contains("Authorization");
        assertThat(json.get("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("Protected endpoint with a garbage token → 401")
    void protectedEndpoint_invalidToken_401() throws Exception {
        Response resp = get("/probe/secure", "not-a-real-jwt");
        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(body(resp).get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("Valid token but insufficient role → 403 with standard Result payload")
    void protectedEndpoint_wrongRole_403() throws Exception {
        String studentToken = tokenFor(7, "bob", UserRole.STUDENT);
        Response resp = get("/probe/admin", studentToken); // /admin requires ADMIN

        assertThat(resp.getStatus()).isEqualTo(403);
        JsonNode json = body(resp);
        assertThat(json.get("code").asInt()).isEqualTo(403);
        assertThat(json.get("msg").asText()).contains("Access denied");
    }

    @Test
    @DisplayName("Valid token with correct role → 200 and UserContext is populated for the resource")
    void protectedEndpoint_correctRole_200_andContextSet() throws Exception {
        String adminToken = tokenFor(1, "alice", UserRole.ADMIN);
        Response resp = get("/probe/admin", adminToken);

        assertThat(resp.getStatus()).isEqualTo(200);
        // The resource echoes UserContext.get().username(), proving the filter populated it.
        assertThat(body(resp).get("data").asText()).isEqualTo("admin-area:alice");
    }

    @Test
    @DisplayName("UserContext is strictly cleared during the response phase (no ThreadLocal leak)")
    void userContext_clearedAfterRequest() {
        String adminToken = tokenFor(1, "alice", UserRole.ADMIN);
        Response resp = get("/probe/secure", adminToken);
        assertThat(resp.getStatus()).isEqualTo(200);

        // The probe filter (priority 100) runs AFTER JwtSecurityFilter's cleanup (priority 1000)
        // in the descending response-filter order, so it must observe a null context.
        assertThat(ContextProbeFilter.OBSERVED_AFTER_CLEANUP.get()).isNull();
    }

    // ── Scenario 2: Result.code → HTTP status line ────────────────────────────

    @Test
    @DisplayName("Result.created() (code 201) is reflected as HTTP 201 Created")
    void resultCode201_mapsToHttp201() throws Exception {
        String token = tokenFor(1, "alice", UserRole.ADMIN);
        Response resp = target("/probe/create").request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .post(jakarta.ws.rs.client.Entity.json("{}"));

        assertThat(resp.getStatus()).isEqualTo(201);          // HTTP status line synced
        assertThat(body(resp).get("code").asInt()).isEqualTo(201); // body unchanged
    }

    @Test
    @DisplayName("Result.ok() (code 200) stays HTTP 200")
    void resultCode200_mapsToHttp200() {
        Response resp = get("/probe/public", null);
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    // ── Scenario 3: Exception mappers ─────────────────────────────────────────

    @Test
    @DisplayName("ServiceException is mapped to its HttpStatus + Result JSON by ServiceExceptionMapper")
    void serviceException_mappedToStatusAndResult() throws Exception {
        String token = tokenFor(1, "alice", UserRole.ADMIN);
        Response resp = get("/probe/service-error", token);

        assertThat(resp.getStatus()).isEqualTo(404);
        JsonNode json = body(resp);
        assertThat(json.get("code").asInt()).isEqualTo(404);
        assertThat(json.get("msg").asText()).contains("widget 42 not found");
        assertThat(json.get("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("Unhandled exception is mapped to HTTP 500 + Result JSON by GeneralExceptionMapper")
    void genericException_mappedTo500Result() throws Exception {
        String token = tokenFor(1, "alice", UserRole.ADMIN);
        Response resp = get("/probe/boom", token);

        assertThat(resp.getStatus()).isEqualTo(500);
        JsonNode json = body(resp);
        assertThat(json.get("code").asInt()).isEqualTo(500);
        assertThat(json.get("msg").asText()).contains("kaboom");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Synthetic test fixtures
    // ─────────────────────────────────────────────────────────────────────────

    @Path("/probe")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class ProbeResource {

        /** Public — no @RoleAccess; echoes "anonymous" when no context is set. */
        @GET
        @Path("/public")
        public Result<String> publicEcho() {
            UserContext.CurrentUser u = UserContext.get();
            return Result.ok(u == null ? "anonymous" : u.username());
        }

        /** Any authenticated role; echoes the authenticated username from UserContext. */
        @GET
        @Path("/secure")
        @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
        public Result<String> secure() {
            return Result.ok(UserContext.get().username());
        }

        /** ADMIN-only; used to assert the 403 path for a STUDENT token. */
        @GET
        @Path("/admin")
        @RoleAccess(UserRole.ADMIN)
        public Result<String> adminOnly() {
            return Result.ok("admin-area:" + UserContext.get().username());
        }

        /** Returns a 201 business code to exercise ResultHttpStatusFilter. */
        @POST
        @Path("/create")
        @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
        public Result<String> create() {
            return Result.created("created");
        }

        /** Throws a domain exception to exercise ServiceExceptionMapper. */
        @GET
        @Path("/service-error")
        @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
        public Result<String> serviceError() {
            throw new ServiceException(HttpStatus.NOT_FOUND, "widget 42 not found");
        }

        /** Throws an unexpected exception to exercise GeneralExceptionMapper. */
        @GET
        @Path("/boom")
        @RoleAccess({UserRole.STUDENT, UserRole.ADMIN})
        public Result<String> boom() {
            throw new IllegalStateException("kaboom");
        }
    }

    /**
     * Test-only response filter that records the {@link UserContext} value it observes.
     * Priority 100 (&lt; JwtSecurityFilter's 1000) means it runs LAST in the descending
     * response-filter order — i.e. after the auth filter has cleared the ThreadLocal —
     * so a non-null observation here would prove a leak.
     */
    @Provider
    @Priority(100)
    public static class ContextProbeFilter implements ContainerResponseFilter {

        static final AtomicReference<Object> OBSERVED_AFTER_CLEANUP = new AtomicReference<>("UNSET");

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) {
            OBSERVED_AFTER_CLEANUP.set(UserContext.get());
        }
    }
}
