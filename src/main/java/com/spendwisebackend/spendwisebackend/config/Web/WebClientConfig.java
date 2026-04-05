package com.spendwisebackend.spendwisebackend.config.Web;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Value("${FASTAPI_BASE_URL}")
    private String fastApiBaseUrl;

    @Value("${FASTAPI_CONNECT_TIMEOUT_MS}")
    private int connectTimeout;

    @Bean
    public WebClient fastApiWebClient() {
        return WebClient.builder()
                .baseUrl(fastApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                // إعدادات التوقيت لضمان عدم تعليق النظام إذا تأخرت خدمة الـ ML
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)))
                .build();
    }
}