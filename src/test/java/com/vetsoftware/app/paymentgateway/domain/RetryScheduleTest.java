package com.vetsoftware.app.paymentgateway.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RetrySchedule")
class RetryScheduleTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 4, 8, 15, 30);

    @Test
    @DisplayName("primer rechazo imputable: reintenta al dia siguiente")
    void primer_rechazo_reintenta_al_dia_siguiente() {
        assertThat(RetrySchedule.nextAttemptAt(0, AHORA)).isEqualTo(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("segundo rechazo imputable: reintenta a los dos dias")
    void segundo_rechazo_reintenta_a_los_dos_dias() {
        assertThat(RetrySchedule.nextAttemptAt(1, AHORA)).isEqualTo(AHORA.plusDays(2));
    }

    @Test
    @DisplayName("tercer rechazo imputable: reintenta a los cuatro dias")
    void tercer_rechazo_reintenta_a_los_cuatro_dias() {
        assertThat(RetrySchedule.nextAttemptAt(2, AHORA)).isEqualTo(AHORA.plusDays(4));
    }

    @Test
    @DisplayName("cuarto rechazo imputable: presupuesto agotado, sin siguiente")
    void cuarto_rechazo_agota_el_presupuesto() {
        assertThat(RetrySchedule.nextAttemptAt(3, AHORA)).isNull();
    }

    @Test
    @DisplayName("mas alla del cuarto: sigue sin siguiente")
    void mas_alla_del_cuarto_sigue_sin_siguiente() {
        assertThat(RetrySchedule.nextAttemptAt(10, AHORA)).isNull();
    }

    @Test
    @DisplayName("un conteo negativo tambien agota, en vez de indexar fuera de rango")
    void un_conteo_negativo_tambien_agota() {
        assertThat(RetrySchedule.nextAttemptAt(-1, AHORA)).isNull();
    }

    @Test
    @DisplayName("MAX_SOFT_ATTEMPTS y RETRY_WINDOW son el espejo de paymentattempt.PaymentAttempt")
    void constantes_espejo() {
        assertThat(RetrySchedule.MAX_SOFT_ATTEMPTS).isEqualTo(4);
        assertThat(RetrySchedule.RETRY_WINDOW).isEqualTo(java.time.Duration.ofDays(14));
    }
}
