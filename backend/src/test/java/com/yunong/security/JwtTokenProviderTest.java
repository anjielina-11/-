package com.yunong.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {
    @Test
    void acceptsPlainUtf8SecretFromApplicationConfiguration() {
        var provider = new JwtTokenProvider(
                "yunnong-platform-secret-key-must-be-at-least-256-bits-long-for-hs256",
                3_600_000,
                86_400_000);

        String token = provider.generateRefreshToken("user-1");

        assertTrue(provider.validateToken(token));
        assertEquals("user-1", provider.getUserIdFromToken(token));
    }
}
