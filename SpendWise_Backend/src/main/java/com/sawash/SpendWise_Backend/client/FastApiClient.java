package com.sawash.SpendWise_Backend.client;

import com.sawash.SpendWise_Backend.dto.FastApiCategorizeRequest;
import com.sawash.SpendWise_Backend.dto.FastApiCategorizeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final WebClient fastApiWebClient;

    public FastApiCategorizeResponse categorize(FastApiCategorizeRequest request) {
        return fastApiWebClient.post()
                .uri("/categorize")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FastApiCategorizeResponse.class)
                .block();
    }
}