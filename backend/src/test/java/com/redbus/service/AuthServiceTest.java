package com.redbus.service;

import com.redbus.config.JwtUtil;
import com.redbus.dto.AuthResponse;
import com.redbus.dto.LoginRequest;
import com.redbus.dto.RegisterRequest;
import com.redbus.entity.User;
import com.redbus.exception.BadRequestException;
import com.redbus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Rohan Verma")
                .email("rohan@example.com")
                .passwordHash("hashedPass")
                .phone("9876543210")
                .role("ROLE_USER")
                .build();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Rohan Verma")
                .email("rohan@example.com")
                .password("password123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail("rohan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("mockToken123");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mockToken123", response.getToken());
        assertEquals("rohan@example.com", response.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Rohan Verma")
                .email("rohan@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("rohan@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .email("rohan@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateToken("rohan@example.com", "ROLE_USER", 1L)).thenReturn("mockToken123");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockToken123", response.getToken());
        assertEquals("Rohan Verma", response.getUser().getName());
    }

    @Test
    void testLoginInvalidPasswordThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("rohan@example.com")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("rohan@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPass")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }
}
