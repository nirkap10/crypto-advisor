package com.moveo.crypto_advisor.auth;

import lombok.Data;

@Data
public class AuthResponse {
    private String message;
    private String token; //jwt token later on
    private boolean onboardingCompleted;
}
