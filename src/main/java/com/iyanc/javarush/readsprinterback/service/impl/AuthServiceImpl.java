package com.iyanc.javarush.readsprinterback.service.impl;

import com.iyanc.javarush.readsprinterback.dto.request.LoginRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RefreshTokenRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RegisterRequest;
import com.iyanc.javarush.readsprinterback.dto.response.AuthResponse;
import com.iyanc.javarush.readsprinterback.dto.response.UserResponse;
import com.iyanc.javarush.readsprinterback.entity.User;
import com.iyanc.javarush.readsprinterback.exception.EmailAlreadyExistsException;
import com.iyanc.javarush.readsprinterback.exception.EmailNotVerifiedException;
import com.iyanc.javarush.readsprinterback.exception.ResourceNotFoundException;
import com.iyanc.javarush.readsprinterback.repository.UserRepository;
import com.iyanc.javarush.readsprinterback.security.JwtUtil;
import com.iyanc.javarush.readsprinterback.service.AuthService;
import com.iyanc.javarush.readsprinterback.service.EmailService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new EmailAlreadyExistsException("Username already in use: " + request.getUsername());
        }

        String token = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .emailVerified(false)
                .verificationToken(token)
                .build();

        userRepository.save(user);
        emailService.sendVerificationEmail(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new JwtException("Invalid or expired refresh token");
        }
        String email = jwtUtil.extractSubject(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user.getEmail()))
                .refreshToken(jwtUtil.generateRefreshToken(user.getEmail()))
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
