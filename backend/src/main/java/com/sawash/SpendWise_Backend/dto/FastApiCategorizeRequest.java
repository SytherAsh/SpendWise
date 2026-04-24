package com.sawash.SpendWise_Backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiCategorizeRequest {
    private String description;
    private BigDecimal amount;

    @JsonProperty("transaction_mode")
    private String transactionMode;

    @JsonProperty("dr_cr_indicator")
    private String drCrIndicator;
}