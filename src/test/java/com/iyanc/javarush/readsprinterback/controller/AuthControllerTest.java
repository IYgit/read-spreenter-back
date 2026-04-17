package com.iyanc.javarush.readsprinterback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyanc.javarush.readsprinterback.dto.request.LoginRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RefreshTokenRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RegisterRequest;
import com.iyanc.javarush.readsprinterback.dto.response.AuthResponse;
import com.iyanc.javarush.readsprinterback.dto.response.UserResponse;
import com.iyanc.javarush.readsprinterback.exception.EmailAlreadyExistsException;
import com.iyanc.javarush.readsprinterback.exception.EmailNotVerifiedException;
import com.iyanc.javarush.readsprinterback.security.JwtUtil;
import com.iyanc.javarush.readsprinterback.security.UserDetailsServiceImpl;
import com.iyanc.javarush.readsprinterback.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration-style MockMvc tests for {@link AuthController} (cases 9.1–9.8).
 * Uses a minimal test security config; AuthService is mocked.
 */
@WebMvcTest(AuthController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=*")
@DisplayName("AuthController (MockMvc)")
class AuthControllerTest {

    /** Minimal security config for the test slice. */
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/verify"
                    ).permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    // Needed to satisfy JwtAuthenticationFilter / UserDetailsServiceImpl beans
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String REFRESH_URL  = "/api/auth/refresh";
    private static final String ME_URL       = "/api/auth/me";

    private AuthResponse sampleAuthResponse() {
        UserResponse user = UserResponse.builder()
                .id(1L).username("testuser").email("test@example.com").role("USER").build();
        return AuthResponse.builder()
                .accessToken("access.token.here")
                .refreshToken("refresh.token.here")
                .user(user)
                .build();
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("secret123");
        return req;
    }

    private LoginRequest validLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("secret123");
        return req;
    }

    private RefreshTokenRequest validRefreshRequest() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("valid.refresh.token");
        return req;
    }

    // ─── 9.1 POST /api/auth/register — валідне тіло → 201 + accessToken ──────

    @Test
    @DisplayName("9.1 POST /api/auth/register — валідне тіло → статус 201, тіло містить message")
    void register_validRequest_returns201WithMessage() throws Exception {
        doNothing().when(authService).register(any());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─── 9.2 POST /api/auth/register — порожній email → 400 ─────────────────

    @Test
    @DisplayName("9.2 POST /api/auth/register — порожній email → статус 400 (Bean Validation)")
    void register_blankEmail_returns400() throws Exception {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("");   // violates @NotBlank @Email

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── 9.3 POST /api/auth/register — EmailAlreadyExistsException → 409 ─────

    @Test
    @DisplayName("9.3 POST /api/auth/register — сервіс кидає EmailAlreadyExistsException → статус 409")
    void register_emailAlreadyExists_returns409() throws Exception {
        doThrow(new EmailAlreadyExistsException("Email already in use"))
                .when(authService).register(any());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ─── 9.4 POST /api/auth/login — валідні дані → 200 + accessToken ─────────

    @Test
    @DisplayName("9.4 POST /api/auth/login — валідні дані → статус 200, тіло містить accessToken")
    void login_validRequest_returns200WithAccessToken() throws Exception {
        when(authService.login(any())).thenReturn(sampleAuthResponse());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token.here"));
    }

    // ─── 9.5 POST /api/auth/login — BadCredentialsException → 401 ────────────

    @Test
    @DisplayName("9.5 POST /api/auth/login — сервіс кидає BadCredentialsException → статус 401")
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ─── 9.6 POST /api/auth/refresh — валідний refreshToken → 200 + нові токени

    @Test
    @DisplayName("9.6 POST /api/auth/refresh — валідний refreshToken → статус 200, нові токени")
    void refresh_validToken_returns200WithNewTokens() throws Exception {
        AuthResponse newTokens = AuthResponse.builder()
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .user(sampleAuthResponse().getUser())
                .build();
        when(authService.refresh(any())).thenReturn(newTokens);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRefreshRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh.token"));
    }

    // ─── 9.7 GET /api/auth/me — авторизований → 200 + дані користувача ───────

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    @DisplayName("9.7 GET /api/auth/me — авторизований запит → статус 200, дані користувача")
    void me_authenticated_returns200() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isOk());
    }

    // ─── 9.8 GET /api/auth/me — без токена → 401 ─────────────────────────────

    @Test
    @DisplayName("9.8 GET /api/auth/me — без токена → статус 401")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());
    }

    // ─── 9.9 POST /api/auth/login — email не верифіковано → 403 ─────────────

    @Test
    @DisplayName("9.9 POST /api/auth/login — email не верифіковано → статус 403")
    void login_emailNotVerified_returns403() throws Exception {
        when(authService.login(any()))
                .thenThrow(new EmailNotVerifiedException("Please verify your email before logging in"));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ─── 9.10 GET /api/auth/verify — валідний токен → 302 redirect ────────────

    @Test
    @DisplayName("9.10 GET /api/auth/verify — валідний токен → 302 redirect до фронтенду")
    void verify_validToken_returns302() throws Exception {
        doNothing().when(authService).verifyEmail(any());

        mockMvc.perform(get("/api/auth/verify").param("token", "valid-uuid-token"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/auth?verified=true")));
    }
}
