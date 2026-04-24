package com.spendwisebackend.spendwisebackend.user.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequset {
    @Schema(description = "email", example = "ahmed@example.com")
    @Email(message = "email not valid")
    @NotEmpty(message = "email requierd")
    private String email;
    @Schema(description = "password", example = "password123", accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "password requierd")
    private String password;
    @NotEmpty
    private String captcha;
    private Boolean rememberMe;
}
