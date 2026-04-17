package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.LoginRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RefreshTokenRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RegisterRequest;
import com.iyanc.javarush.readsprinterback.dto.response.AuthResponse;

public interface AuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void verifyEmail(String token);
}

