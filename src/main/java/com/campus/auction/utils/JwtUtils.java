package com.campus.auction.utils;

import com.campus.auction.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Thin wrapper around JJWT 0.12.x for generating and parsing signed JWTs.
 *
 * <p>Token payload:
 * <pre>
 * {
 *   "userId"   : 1,
 *   "username" : "alice",
 *   "role"     : "STUDENT",
 *   "iat"      : &lt;issued-at-epoch-seconds&gt;,
 *   "exp"      : &lt;expiry-epoch-seconds&gt;
 * }
 * </pre>
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expirationMs;

    // Build the signing key lazily from the injected secret string.
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Creates a signed JWT that carries the user's id, username, and role. */
    public String generateToken(UserDTO user) {
        return Jwts.builder()
                .claim("userId",   user.getId())
                .claim("username", user.getUsername())
                .claim("role",     user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    /**
     * Validates the signature and expiry, then returns the decoded claims.
     *
     * @throws io.jsonwebtoken.JwtException on any validation failure
     *         (expired, malformed, bad signature, etc.)
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
