package com.vetsoftware.app.infrastructure.pdf;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PdfProperties.class)
public class PdfConfig {

    @Bean(name = "gotenbergRestClient")
    public RestClient gotenbergRestClient(PdfProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(factory)
                .build();
    }
}
