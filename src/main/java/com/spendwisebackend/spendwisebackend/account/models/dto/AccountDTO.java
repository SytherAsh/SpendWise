package com.spendwisebackend.spendwisebackend.account.models.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private UUID id;

    @NotBlank(message = "اسم البنك مطلوب")
    @Size(min = 3, max = 100, message = "اسم البنك يجب أن يكون بين 3 و 100 حرف")
    private String bankName;

    @NotBlank(message = "نوع الحساب مطلوب")
    private String accountType;

    private ZonedDateTime createdAt;
}