package com.vetsoftware.app.electronicdocument.infrastructure.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * DianHttpConfig — el {@code @Bean} en si mismo es codigo de infraestructura
 * puro (arma un {@link RestClient} con timeouts fijos): se instancia y se
 * invoca directamente, sin levantar contexto de Spring.
 */
@DisplayName("DianHttpConfig")
class DianHttpConfigTest {

    private final DianHttpConfig config = new DianHttpConfig();

    @Test
    @DisplayName("construye un RestClient utilizable a partir del builder")
    void construye_un_restclient_utilizable() {
        RestClient client = config.dianRestClient(RestClient.builder());

        assertThat(client).isNotNull();
    }

    @Test
    @DisplayName("cada invocacion construye una instancia propia, no comparte estado entre llamadas")
    void cada_invocacion_construye_una_instancia_propia() {
        RestClient primero = config.dianRestClient(RestClient.builder());
        RestClient segundo = config.dianRestClient(RestClient.builder());

        assertThat(primero).isNotSameAs(segundo);
    }
}
