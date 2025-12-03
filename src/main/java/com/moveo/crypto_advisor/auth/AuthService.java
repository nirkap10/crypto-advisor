package com.moveo.crypto_advisor.auth;

import com.moveo.crypto_advisor.preferences.PreferencesService;
import com.moveo.crypto_advisor.user.User;
import com.moveo.crypto_advisor.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PreferencesService preferencesService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setMessage("User registered successfully");
        response.setOnboardingCompleted(false);
        return response;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        boolean completed = preferencesService.hasCompletedOnboarding(user.getUsername());

        AuthResponse response = new AuthResponse();
        response.setMessage("Login successful");
        response.setOnboardingCompleted(completed);
        return response;
    }
}
