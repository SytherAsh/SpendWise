package com.spendwisebackend.spendwisebackend.user.models.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private UUID id;
    private String password;
    private String name;
    private String email;
    private String role;
    private Boolean active;
}
