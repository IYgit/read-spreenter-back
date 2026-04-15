package com.iyanc.javarush.readsprinterback.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtUtil}.
 * No Spring context — JwtUtil is instantiated directly via its constructor.
 *
 * Secret keys must be ≥ 32 bytes (256 bits) for HMAC-SHA-256.
 */
@DisplayName("JwtUtil")
class JwtUtilTest {

    /** Primary secret — 51 chars = 51 bytes, satisfies HMAC-SHA-256 requirement. */
    private static final String SECRET =
            "test-secret-key-that-is-long-enough-for-hmac-sha256";

    /** Different secret for cross-signature tests — 47 bytes. */
    private static final String OTHER_SECRET =
            "other-secret-key-that-is-long-enough-for-tests!";

    private static final long ACCESS_EXPIRY  = 900_000L;       // 15 min
    private static final long REFRESH_EXPIRY = 604_800_000L;   // 7 days

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    }

    // ─── 1.1 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.1 generateAccessToken — повертає непорожній рядок")
    void generateAccessToken_returnsNonBlankString() {
        String token = jwtUtil.generateAccessToken("user@test.com");

        assertThat(token).isNotBlank();
    }

    // ─── 1.2 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.2 generateAccessToken — claim 'type' = 'access'")
    void generateAccessToken_claimTypeIsAccess() {
        String token = jwtUtil.generateAccessToken("user@test.com");

        assertThat(jwtUtil.isAccessToken(token)).isTrue();
    }

    // ─── 1.3 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.3 generateRefreshToken — claim 'type' = 'refresh'")
    void generateRefreshToken_claimTypeIsRefresh() {
        String token = jwtUtil.generateRefreshToken("user@test.com");

        assertThat(jwtUtil.isAccessToken(token)).isFalse();
    }

    // ─── 1.4 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.4 extractSubject — повертає email, вбудований як subject")
    void extractSubject_returnsEmbeddedEmail() {
        String email = "user@test.com";
        String token = jwtUtil.generateAccessToken(email);

        assertThat(jwtUtil.extractSubject(token)).isEqualTo(email);
    }

    // ─── 1.5 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.5 validateToken — валідний нещодавно згенерований токен → true")
    void validateToken_freshToken_returnsTrue() {
        String token = jwtUtil.generateAccessToken("user@test.com");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    // ─── 1.6 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.6 validateToken — рядок 'invalid.token.string' → false")
    void validateToken_malformedString_returnsFalse() {
        assertThat(jwtUtil.validateToken("invalid.token.string")).isFalse();
    }

    @Test
    @DisplayName("1.6b validateToken — порожній рядок → false")
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    // ─── 1.7 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.7 validateToken — токен підписаний іншим секретом → false")
    void validateToken_tokenSignedWithDifferentSecret_returnsFalse() {
        JwtUtil otherJwtUtil = new JwtUtil(OTHER_SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
        String foreignToken  = otherJwtUtil.generateAccessToken("user@test.com");

        assertThat(jwtUtil.validateToken(foreignToken)).isFalse();
    }

    // ─── 1.8 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.8 isAccessToken — access-токен → true")
    void isAccessToken_accessToken_returnsTrue() {
        String token = jwtUtil.generateAccessToken("user@test.com");

        assertThat(jwtUtil.isAccessToken(token)).isTrue();
    }

    // ─── 1.9 ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1.9 isAccessToken — refresh-токен → false")
    void isAccessToken_refreshToken_returnsFalse() {
        String token = jwtUtil.generateRefreshToken("user@test.com");

        assertThat(jwtUtil.isAccessToken(token)).isFalse();
    }

    // ─── Додаткові edge-case тести ────────────────────────────────────────────

    @Test
    @DisplayName("extractSubject — access і refresh токени зберігають однаковий subject")
    void extractSubject_sameEmailForAccessAndRefresh() {
        String email = "another@example.com";

        assertThat(jwtUtil.extractSubject(jwtUtil.generateAccessToken(email)))
                .isEqualTo(email);
        assertThat(jwtUtil.extractSubject(jwtUtil.generateRefreshToken(email)))
                .isEqualTo(email);
    }

    @Test
    @DisplayName("generateAccessToken та generateRefreshToken — повертають різні токени для одного subject")
    void accessAndRefreshTokens_areDifferent() {
        String email = "user@test.com";
        String accessToken  = jwtUtil.generateAccessToken(email);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }
}

