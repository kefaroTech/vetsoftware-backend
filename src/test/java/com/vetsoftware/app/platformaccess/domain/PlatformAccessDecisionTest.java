package com.vetsoftware.app.platformaccess.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La columna {@code decision} es un {@code VARCHAR(10)} con un {@code CHECK},
 * no un {@code ENUM} de MySQL. Esta clase es la frontera entre ese texto y el
 * tipo, y por eso su camino de fallo importa: una fila con un valor que el
 * {@code CHECK} no vio —escrita por una migración o por SQL manual— tiene que
 * reventar al leerse, no colarse como {@code null} y hacer pasar por pendiente
 * una solicitud ya decidida.
 */
@DisplayName("PlatformAccessDecision — el texto de la columna y el tipo")
class PlatformAccessDecisionTest {

    @ParameterizedTest
    @EnumSource(PlatformAccessDecision.class)
    @DisplayName("toda decision del enum sobrevive a la ida y vuelta por su nombre")
    void toda_decision_sobrevive_a_la_ida_y_vuelta(PlatformAccessDecision decision) {
        assertThat(PlatformAccessDecision.fromNullable(decision.name())).isEqualTo(decision);
    }

    @Test
    @DisplayName("solo existen dos decisiones: aprobar y rechazar")
    void solo_existen_dos_decisiones() {
        assertThat(PlatformAccessDecision.values()).containsExactly(PlatformAccessDecision.APPROVED,
                PlatformAccessDecision.REJECTED);
    }

    @Test
    @DisplayName("null se traduce a null: es la solicitud todavia sin decidir")
    void null_se_traduce_a_null() {
        assertThat(PlatformAccessDecision.fromNullable(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("el texto en blanco se trata como sin decidir")
    void el_texto_en_blanco_se_trata_como_sin_decidir(String valor) {
        assertThat(PlatformAccessDecision.fromNullable(valor)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAYBE", "approved", "Approved", "APPROVED "})
    @DisplayName("un valor desconocido revienta en vez de degradar a sin decidir")
    void un_valor_desconocido_revienta(String valor) {
        // Degradar a null convertiria una solicitud ya resuelta en pendiente y
        // permitiria decidirla otra vez.
        assertThatThrownBy(() -> PlatformAccessDecision.fromNullable(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown platform access decision");
    }
}
