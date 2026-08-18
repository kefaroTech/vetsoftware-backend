package com.vetsoftware.app.dianprovider.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DianProviderConfigNotFoundException")
class DianProviderConfigNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la empresa que no tiene config")
    void el_mensaje_incluye_el_id_de_la_empresa() {
        DianProviderConfigNotFoundException ex = new DianProviderConfigNotFoundException(9L);

        assertThat(ex.getMessage()).contains("9");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
