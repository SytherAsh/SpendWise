package com.sawash.SpendWise_Backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FastApiCategorizeResponse {
    private String category;
    private BigDecimal confidence;

    @JsonProperty("model_version")
    private String modelVersion;
}