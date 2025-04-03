package com.ecommerce.authservice.service;


import com.ecommerce.authservice.dto.AuthRequest;
import com.ecommerce.authservice.dto.AuthResponse;
import com.ecommerce.authservice.dto.RegisterRequest;

public interface AuthService {

    AuthResponse login(AuthRequest authRequest);

    AuthResponse register(RegisterRequest registerRequest);

    AuthResponse refreshToken(String refreshToken);
}
