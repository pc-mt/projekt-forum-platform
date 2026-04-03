package com.example.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

public final class JwtUtils {

    private static final String ISSUER = "chat-platform-backend";
    private static final long TOKEN_HOURS = 24;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtUtils() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = "dev-secret-change-in-production";
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
    }

    public String generateToken(long userId, String email, String role) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(userId))
                .withClaim("email", email)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(TOKEN_HOURS, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("invalid token");
        }
    }

    public long extractUserId(String token) {
        DecodedJWT decoded = verifyToken(token);
        return Long.parseLong(decoded.getSubject());
    }
}

