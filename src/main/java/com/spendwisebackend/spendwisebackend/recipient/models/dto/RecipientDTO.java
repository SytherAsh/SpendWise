package com.spendwisebackend.spendwisebackend.recipient.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientDTO {

    private UUID id;

    @NotBlank(message = "اسم المستلم مطلوب")
    @Size(min = 2, max = 150, message = "اسم المستلم يجب أن يكون بين 2 و 150 حرف")
    private String name;

    @NotBlank(message = "معرف الدفع (UPI ID) مطلوب")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$", message = "صيغة UPI ID غير صحيحة (مثال: user@bank)")
    private String upiId;

    @NotBlank(message = "اسم البنك التابع للمستلم مطلوب")
    private String bankName;

    private ZonedDateTime createdAt;
}
