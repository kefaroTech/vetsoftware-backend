package com.vetsoftware.app.paymentgateway.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("WompiProperties")
class WompiPropertiesTest {

    @Test
    @DisplayName("habilitado con las cuatro credenciales se construye")
    void habilitado_con_credenciales_se_construye() {
        assertThatCode(() -> propiedades(true, "pub_test_x", "prv_test_x", "integrity", "events"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deshabilitado admite credenciales vacías: el cliente falla cerrado en runtime")
    void deshabilitado_admite_credenciales_vacias() {
        assertThatCode(() -> propiedades(false, "", "", "", "")).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "habilitado sin {0} no se construye")
    @CsvSource({"public-key, '', prv_test_x, integrity, events",
            "private-key, pub_test_x, '', integrity, events",
            "integrity-secret, pub_test_x, prv_test_x, '', events",
            "events-secret, pub_test_x, prv_test_x, integrity, ' '"})
    void habilitado_sin_una_credencial_no_se_construye(String propiedad, String publicKey,
            String privateKey, String integritySecret, String eventsSecret) {
        assertThatThrownBy(
                () -> propiedades(true, publicKey, privateKey, integritySecret, eventsSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vetsoftware.payments.wompi." + propiedad);
    }

    private static WompiProperties propiedades(boolean enabled, String publicKey, String privateKey,
            String integritySecret, String eventsSecret) {
        return new WompiProperties(enabled, "https://sandbox.wompi.test/v1", publicKey, privateKey,
                integritySecret, eventsSecret, 6, Duration.ofSeconds(2), Duration.ofHours(24),
                65536L, Duration.ofHours(24));
    }
}
