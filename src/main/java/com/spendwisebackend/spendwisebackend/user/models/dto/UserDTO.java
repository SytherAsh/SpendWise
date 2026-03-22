package com.spendwisebackend.spendwisebackend.user.models.dto;

import java.util.UUID;
import com.spendwisebackend.spendwisebackend.user.models.OnCreate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    @Schema(description = "id user", example = "105", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "full name", example = "name a")
    @NotBlank(message = "name is not empity")
    @Size(min = 3, max = 50, message = "name between 3 - 50")
    private String name;

    @Schema(description = "email", example = "ahmed@example.com")
    @Email(message = "email invaild")
    @NotBlank(message = "email required")
    private String email;

    @Schema(description = "password", example = "password123", accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "password required", groups = OnCreate.class)
    private String password;

    @Schema(description = "phone", example = "0123456789")
    @NotBlank(message = "phone required", groups = OnCreate.class)
    private String phone;

    @Schema(description = "role user", example = "ADMIN, CLIENT, PROVIDER")
    @NotBlank(message = "role user required")
    @Pattern(regexp = "^(ADMIN|CLIENT|PROVIDER)$", message = "enter valid value : ADMIN, CLIENT, PROVIDER")
    private String role;
    private Boolean active;
}