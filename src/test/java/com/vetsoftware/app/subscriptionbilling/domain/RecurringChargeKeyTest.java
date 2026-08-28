package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La llave antiduplicados del barrido.
 *
 * <p>
 * <b>Esta prueba no es opcional</b>: no hay indice unico detras de esta regla
 * —{@code subscription_charges} no lleva columna de idempotencia— asi que lo
 * unico que impide que un reinicio cobre dos veces es este tipo y la consulta
 * que lo usa.
 */
@DisplayName("RecurringChargeKey — la llave que evita el cobro doble tras un reinicio")
class RecurringChargeKeyTest {

    private static final ServicePeriod MARZO = new ServicePeriod(LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31));

    @Test
    @DisplayName("la misma linea en el mismo periodo produce la misma llave")
    void misma_linea_mismo_periodo_misma_llave() {
        assertThat(RecurringChargeKey.of(42L, 7L, 900L, MARZO))
                .isEqualTo(RecurringChargeKey.of(42L, 7L, 900L, MARZO));
    }

    /**
     * <b>El caso que ya obligo a corregir el documento una vez.</b> Con tramos
     * acumulativos, un mismo {@code catalog_item_id} tiene dos lineas vivas en el
     * mismo periodo —los primeros N a una tarifa y el resto a otra—. Si la llave
     * agrupara por articulo, la segunda linea se veria como duplicada de la primera
     * y <b>no se cobraria</b>: media factura, en silencio y todos los meses.
     */
    @Test
    @DisplayName("dos tramos del mismo articulo en el mismo periodo dan llaves distintas")
    void dos_tramos_del_mismo_articulo_dan_llaves_distintas() {
        RecurringChargeKey tramoUno = RecurringChargeKey.of(42L, 7L, 900L, MARZO);
        RecurringChargeKey tramoDos = RecurringChargeKey.of(42L, 7L, 901L, MARZO);

        assertThat(tramoUno).isNotEqualTo(tramoDos);
        assertThat(tramoUno.value()).isNotEqualTo(tramoDos.value());
    }

    @Test
    @DisplayName("la misma linea en dos periodos distintos da llaves distintas")
    void periodos_distintos_dan_llaves_distintas() {
        ServicePeriod abril = new ServicePeriod(LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30));

        assertThat(RecurringChargeKey.of(42L, 7L, 900L, MARZO))
                .isNotEqualTo(RecurringChargeKey.of(42L, 7L, 900L, abril));
    }

    @Test
    @DisplayName("la misma linea de otra empresa da otra llave")
    void empresas_distintas_dan_llaves_distintas() {
        assertThat(RecurringChargeKey.of(42L, 7L, 900L, MARZO))
                .isNotEqualTo(RecurringChargeKey.of(99L, 7L, 900L, MARZO));
    }

    @Test
    @DisplayName("una llave sin linea es un error, no un caso general")
    void sin_linea_revienta() {
        assertThatThrownBy(() -> new RecurringChargeKey(42L, 7L, null, MARZO.start(), MARZO.end()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subscriptionItemId is required");
    }

    @Test
    @DisplayName("devuelve el periodo que cubre como tipo de dominio")
    void devuelve_su_periodo() {
        assertThat(RecurringChargeKey.of(42L, 7L, 900L, MARZO).servicePeriod()).isEqualTo(MARZO);
    }

    @Test
    @DisplayName("la forma textual nombra empresa, contrato, linea y periodo")
    void la_forma_textual_lleva_los_cinco_datos() {
        assertThat(RecurringChargeKey.of(42L, 7L, 900L, MARZO).value())
                .isEqualTo("recurring:42:7:900:2026-03-01:2026-03-31");
    }
}
