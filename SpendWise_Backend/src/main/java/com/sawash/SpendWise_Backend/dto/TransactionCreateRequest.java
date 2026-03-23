package com.sawash.SpendWise_Backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransactionCreateRequest {
    @NotBlank
    private String transactionReference;
    private String transactionDate;
    private BigDecimal amount;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private String transactionMode;
    private String drCrIndicator;
    private String note;
    private String recipientName;
    private String bank;
    private String upiId;
}