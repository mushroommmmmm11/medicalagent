package com.medlab.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(PythonOcrServiceProperties.class)
public class WebClientConfig {

    @Bean("pythonOcrWebClient")
    public WebClient pythonOcrWebClient(PythonOcrServiceProperties pythonOcrServiceProperties) {
        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        pythonOcrServiceProperties.getConnectTimeoutMillis()
                )
                .responseTimeout(java.time.Duration.ofSeconds(
                        pythonOcrServiceProperties.getResponseTimeoutSeconds()
                ))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(
                                pythonOcrServiceProperties.getReadTimeoutSeconds(),
                                TimeUnit.SECONDS
                        ))
                        .addHandlerLast(new WriteTimeoutHandler(
                                pythonOcrServiceProperties.getWriteTimeoutSeconds(),
                                TimeUnit.SECONDS
                        )));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
                        .maxInMemorySize(pythonOcrServiceProperties.getMaxInMemorySize()))
                .build();

        return WebClient.builder()
                .baseUrl(pythonOcrServiceProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "MedLabAgent-JavaBackend/1.0.0")
                .build();
    }

    @Bean("genericWebClient")
    public WebClient genericWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(java.time.Duration.ofSeconds(30));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
