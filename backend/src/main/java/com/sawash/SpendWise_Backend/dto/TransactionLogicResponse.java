package com.sawash.SpendWise_Backend.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionLogicResponse {
    UUID transactionId;
    String transactionReference;
    String direction;
    BigDecimal effectiveAmount;
    String sizeBucket;
    String transactionMode;
    String note;
}