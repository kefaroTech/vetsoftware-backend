package com.vetsoftware.app.dianprovider.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("ProviderType — proveedor tecnologico DIAN")
class ProviderTypeTest {

    @Test
    @DisplayName("hoy solo existe MATIAS: un valor nuevo debe revisar este test")
    void hoy_solo_existe_matias() {
        assertThat(ProviderType.values()).containsExactly(ProviderType.MATIAS);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ProviderType.class)
    @DisplayName("isAsynchronous refleja si el resultado DIAN llega por webhook")
    void is_asynchronous_refleja_si_el_resultado_llega_por_webhook(ProviderType provider) {
        // MATIAS es el unico proveedor hoy y es asincrono (webhook). El
        // EnumSource deja este test listo para cazar la rama que falte el dia
        // que se agregue un segundo proveedor sincrono.
        assertThat(provider.isAsynchronous()).isTrue();
    }
}
