package com.sawash.SpendWise_Backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionResponse {
    UUID id;
    UUID accountId;
    UUID recipientId;
    String transactionReference;
    LocalDate transactionDate;
    BigDecimal amount;
    BigDecimal debit;
    BigDecimal credit;
    BigDecimal balance;
    String transactionMode;
    String drCrIndicator;
    String note;
}