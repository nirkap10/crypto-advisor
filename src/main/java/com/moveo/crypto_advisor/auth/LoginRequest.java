package com.moveo.crypto_advisor.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
