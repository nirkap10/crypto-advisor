package com.moveo.crypto_advisor.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
}
