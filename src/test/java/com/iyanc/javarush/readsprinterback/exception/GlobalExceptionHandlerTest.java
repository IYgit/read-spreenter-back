package com.iyanc.javarush.readsprinterback.exception;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * Methods are invoked directly — no Spring context needed.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── 8.1 ResourceNotFoundException → 404 ─────────────────────────────────

    @Test
    @DisplayName("8.1 ResourceNotFoundException → HTTP 404, status=404")
    void handleNotFound_returns404() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleNotFound(new ResourceNotFoundException("Text not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Text not found");
    }

    // ─── 8.2 EmailAlreadyExistsException → 409 ───────────────────────────────

    @Test
    @DisplayName("8.2 EmailAlreadyExistsException → HTTP 409, status=409")
    void handleEmailExists_returns409() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleEmailExists(new EmailAlreadyExistsException("Email already in use"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("Email already in use");
    }

    // ─── 8.3 BadCredentialsException → 401 ───────────────────────────────────

    @Test
    @DisplayName("8.3 BadCredentialsException → HTTP 401, status=401")
    void handleBadCredentials_returns401() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("bad creds"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    // ─── 8.4 JwtException → 401 ──────────────────────────────────────────────

    @Test
    @DisplayName("8.4 JwtException → HTTP 401, status=401")
    void handleJwtException_returns401() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleJwtException(new MalformedJwtException("bad jwt"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    // ─── 8.5 MethodArgumentNotValidException → 400 + details ─────────────────

    @Test
    @DisplayName("8.5 MethodArgumentNotValidException → HTTP 400, status=400, details містить поля форми")
    void handleValidation_returns400WithDetails() {
        FieldError fieldError = new FieldError("registerRequest", "email", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getDetails())
                .isNotNull()
                .containsKey("email")
                .containsEntry("email", "must not be blank");
    }

    // ─── 8.6 загальний Exception → 500 ───────────────────────────────────────

    @Test
    @DisplayName("8.6 Exception (загальний) → HTTP 500, status=500")
    void handleGeneral_returns500() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGeneral(new RuntimeException("unexpected error"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}

