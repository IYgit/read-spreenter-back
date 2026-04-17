package com.iyanc.javarush.readsprinterback.service.impl;

import com.iyanc.javarush.readsprinterback.dto.request.LoginRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RefreshTokenRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RegisterRequest;
import com.iyanc.javarush.readsprinterback.dto.response.AuthResponse;
import com.iyanc.javarush.readsprinterback.entity.User;
import com.iyanc.javarush.readsprinterback.exception.EmailAlreadyExistsException;
import com.iyanc.javarush.readsprinterback.exception.EmailNotVerifiedException;
import com.iyanc.javarush.readsprinterback.exception.ResourceNotFoundException;
import com.iyanc.javarush.readsprinterback.repository.UserRepository;
import com.iyanc.javarush.readsprinterback.security.JwtUtil;
import com.iyanc.javarush.readsprinterback.service.EmailService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthServiceImpl}.
 * All dependencies are replaced with Mockito mocks — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository      userRepository;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtUtil             jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmailService        emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ─── Test fixtures ────────────────────────────────────────────────────────

    private static final String EMAIL    = "test@example.com";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";
    private static final String HASHED   = "hashed_password";
    private static final String A_TOKEN  = "access.token";
    private static final String R_TOKEN  = "refresh.token";

    private RegisterRequest     registerRequest;
    private User                persistedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername(USERNAME);
        registerRequest.setEmail(EMAIL);
        registerRequest.setPassword(PASSWORD);

        persistedUser = User.builder()
                .id(1L)
                .username(USERNAME)
                .email(EMAIL)
                .passwordHash(HASHED)
                .role(User.Role.USER)
                .emailVerified(true)
                .build();
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2.1 register — новий email + username → user збережено, email відправлено")
    void register_newUser_savesUserAndSendsEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED);
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        authService.register(registerRequest);

        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(any(User.class));
    }

    @Test
    @DisplayName("2.2 register — email вже існує → EmailAlreadyExistsException з повідомленням про email")
    void register_emailAlreadyExists_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(EMAIL);

        // username check must NOT be reached
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("2.3 register — username вже існує → EmailAlreadyExistsException з повідомленням про username")
    void register_usernameAlreadyExists_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(USERNAME);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("2.4 register — пароль хешується через passwordEncoder рівно 1 раз")
    void register_passwordIsEncodedExactlyOnce() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED);
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        authService.register(registerRequest);

        verify(passwordEncoder, times(1)).encode(PASSWORD);
    }

    @Test
    @DisplayName("2.4b register — збережений User містить хешований пароль, не plain-text")
    void register_savedUserContainsHashedPassword() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED);
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);

        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo(HASHED);
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2.5 login — валідні дані → authenticate() викликається, повертає AuthResponse")
    void login_validCredentials_callsAuthManagerAndReturnsResponse() {
        LoginRequest req = loginRequest(EMAIL, PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser));
        when(jwtUtil.generateAccessToken(EMAIL)).thenReturn(A_TOKEN);
        when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn(R_TOKEN);

        AuthResponse response = authService.login(req);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD));
        assertThat(response.getAccessToken()).isEqualTo(A_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(R_TOKEN);
        assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("2.6 login — користувач не знайдений у репозиторії → ResourceNotFoundException")
    void login_userNotInRepository_throwsResourceNotFoundException() {
        LoginRequest req = loginRequest("missing@example.com", PASSWORD);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── refresh ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2.7 refresh — валідний refresh-токен → AuthResponse з новими токенами")
    void refresh_validToken_returnsNewAuthResponse() {
        RefreshTokenRequest req = refreshRequest("valid.refresh.token");
        when(jwtUtil.validateToken("valid.refresh.token")).thenReturn(true);
        when(jwtUtil.extractSubject("valid.refresh.token")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser));
        when(jwtUtil.generateAccessToken(EMAIL)).thenReturn("new." + A_TOKEN);
        when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn("new." + R_TOKEN);

        AuthResponse response = authService.refresh(req);

        assertThat(response.getAccessToken()).isEqualTo("new." + A_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo("new." + R_TOKEN);
        assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("2.7b refresh — нові токени відрізняються від старого refresh-токена")
    void refresh_newTokensAreDifferentFromOldRefreshToken() {
        String oldRefresh = "old.refresh.token";
        RefreshTokenRequest req = refreshRequest(oldRefresh);
        when(jwtUtil.validateToken(oldRefresh)).thenReturn(true);
        when(jwtUtil.extractSubject(oldRefresh)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser));
        when(jwtUtil.generateAccessToken(EMAIL)).thenReturn("brand.new.access");
        when(jwtUtil.generateRefreshToken(EMAIL)).thenReturn("brand.new.refresh");

        AuthResponse response = authService.refresh(req);

        assertThat(response.getAccessToken()).isNotEqualTo(oldRefresh);
        assertThat(response.getRefreshToken()).isNotEqualTo(oldRefresh);
    }

    @Test
    @DisplayName("2.8 refresh — невалідний refresh-токен → JwtException")
    void refresh_invalidToken_throwsJwtException() {
        RefreshTokenRequest req = refreshRequest("invalid.token");
        when(jwtUtil.validateToken("invalid.token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(JwtException.class);

        // subject must NOT be extracted for invalid token
        verify(jwtUtil, never()).extractSubject(any());
    }

    @Test
    @DisplayName("2.9 refresh — токен валідний, але email не знайдено → ResourceNotFoundException")
    void refresh_validTokenButUserNotFound_throwsResourceNotFoundException() {
        RefreshTokenRequest req = refreshRequest("valid.orphan.token");
        when(jwtUtil.validateToken("valid.orphan.token")).thenReturn(true);
        when(jwtUtil.extractSubject("valid.orphan.token")).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private static RefreshTokenRequest refreshRequest(String token) {
        RefreshTokenRequest r = new RefreshTokenRequest();
        r.setRefreshToken(token);
        return r;
    }

    // ─── login — email not verified ──────────────────────────────────────────

    @Test
    @DisplayName("2.5b login — email не верифіковано → EmailNotVerifiedException")
    void login_emailNotVerified_throwsEmailNotVerifiedException() {
        User unverifiedUser = User.builder()
                .id(2L).username(USERNAME).email(EMAIL)
                .passwordHash(HASHED).role(User.Role.USER)
                .emailVerified(false).build();
        LoginRequest req = loginRequest(EMAIL, PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    // ─── verifyEmail ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("2.10 verifyEmail — валідний токен → emailVerified=true, token=null")
    void verifyEmail_validToken_setsEmailVerifiedTrue() {
        User unverifiedUser = User.builder()
                .id(3L).username(USERNAME).email(EMAIL)
                .passwordHash(HASHED).role(User.Role.USER)
                .emailVerified(false).verificationToken("uuid-token").build();
        when(userRepository.findByVerificationToken("uuid-token")).thenReturn(Optional.of(unverifiedUser));

        authService.verifyEmail("uuid-token");

        assertThat(unverifiedUser.isEmailVerified()).isTrue();
        assertThat(unverifiedUser.getVerificationToken()).isNull();
        verify(userRepository).save(unverifiedUser);
    }

    @Test
    @DisplayName("2.11 verifyEmail — невалідний токен → ResourceNotFoundException")
    void verifyEmail_invalidToken_throwsResourceNotFoundException() {
        when(userRepository.findByVerificationToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

