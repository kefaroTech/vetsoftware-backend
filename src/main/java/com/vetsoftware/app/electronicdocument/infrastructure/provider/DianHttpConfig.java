package com.vetsoftware.app.electronicdocument.infrastructure.provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP compartido para los proveedores DIAN. SIN base URL fija: cada llamada usa la URL base
 * de la config de la empresa (ProviderConfigSnapshot.baseUrl). Timeouts holgados porque la validación
 * síncrona (Factus) puede tardar.
 */
@Configuration
public class DianHttpConfig {

    @Bean(name = "dianRestClient")
    public RestClient dianRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(60_000);
        return RestClient.builder().requestFactory(factory).build();
    }
}
