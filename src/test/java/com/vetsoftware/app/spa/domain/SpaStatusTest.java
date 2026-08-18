package com.vetsoftware.app.spa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("SpaStatus")
class SpaStatusTest {

    @Test
    @DisplayName("tiene exactamente los tres estados del flujo de agenda")
    void tiene_los_tres_estados_del_flujo() {
        assertThat(SpaStatus.values()).containsExactly(SpaStatus.AGENDADA, SpaStatus.COMPLETADO,
                SpaStatus.CANCELADO);
    }

    @ParameterizedTest
    @EnumSource(SpaStatus.class)
    @DisplayName("cada estado hace ida y vuelta por su nombre")
    void cada_estado_hace_ida_y_vuelta_por_su_nombre(SpaStatus status) {
        assertThat(SpaStatus.valueOf(status.name())).isEqualTo(status);
    }
}
