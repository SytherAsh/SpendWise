package com.spendwisebackend.spendwisebackend.transaction.models.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private UUID id;
    
    @NotNull(message = "يجب تحديد الحساب")
    private UUID accountId;

    @NotNull(message = "يجب تحديد المستلم")
    private UUID recipientId;

    @NotBlank(message = "رقم المرجع للعملية مطلوب")
    private String transactionReference;

    @NotNull(message = "تاريخ العملية مطلوب")
    @PastOrPresent(message = "تاريخ العملية لا يمكن أن يكون في المستقبل")
    private LocalDate transactionDate;

    @NotNull(message = "المبلغ مطلوب")
    @Positive(message = "يجب أن يكون المبلغ رقماً موجباً")
    @Digits(integer = 12, fraction = 2, message = "المبلغ يتجاوز الحد المسموح به")
    private BigDecimal amount;

    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;

    @NotBlank(message = "وسيلة التحويل مطلوبة (كاش، شبكة، إلخ)")
    private String transactionMode;

    @Pattern(regexp = "^(DR|CR)$", message = "المؤشر يجب أن يكون إما DR أو CR")
    private String drCrIndicator;

    private String note;
    private ZonedDateTime createdAt;
}