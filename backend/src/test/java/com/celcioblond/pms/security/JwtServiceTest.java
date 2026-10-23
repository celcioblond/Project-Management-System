package com.celcioblond.pms.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("a-test-secret-that-is-at-least-32-bytes!".getBytes());

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

    @Test
    void generatedTokenResolvesToItsUsername() {
        String token = jwtService.generateToken("alice");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("alice");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.extractUsername(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String otherSecret = Base64.getEncoder()
                .encodeToString("another-secret-that-is-at-least-32-bytes".getBytes());
        String foreignToken = new JwtService(otherSecret, Duration.ofHours(1)).generateToken("alice");

        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = new JwtService(SECRET, Duration.ofSeconds(-5)).generateToken("alice");

        assertThatThrownBy(() -> jwtService.extractUsername(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    void secretShorterThan256BitsFailsAtStartup() {
        String weak = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new JwtService(weak, Duration.ofHours(1))).isInstanceOf(WeakKeyException.class);
    }
}
