package com.vetsoftware.app.surgery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("SurgeryStatus")
class SurgeryStatusTest {

    @ParameterizedTest
    @EnumSource(SurgeryStatus.class)
    @DisplayName("cada estado se reconstruye desde su nombre persistido")
    void cada_estado_se_reconstruye_desde_su_nombre_persistido(SurgeryStatus estado) {
        assertThat(SurgeryStatus.valueOf(estado.name())).isSameAs(estado);
    }

    @Test
    @DisplayName("el catalogo es el pactado con la agenda quirurgica")
    void el_catalogo_es_el_pactado() {
        assertThat(SurgeryStatus.values()).containsExactly(SurgeryStatus.PROGRAMADA,
                SurgeryStatus.PENDIENTE, SurgeryStatus.COMPLETADO, SurgeryStatus.CANCELADO);
    }

    @Test
    @DisplayName("un estado desconocido no se puede materializar")
    void un_estado_desconocido_no_se_puede_materializar() {
        assertThatThrownBy(() -> SurgeryStatus.valueOf("EN_CURSO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No enum constant");
    }
}
