package com.vetsoftware.app.companytrialgrant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("TrialPolicyOutcome — la traducción entre los tres vocabularios, escrita una vez")
class TrialPolicyOutcomeTest {

    @Test
    @DisplayName("R-TRIAL-20 · cada uno de los tres desenlaces produce exactamente su terna:"
            + " política, modo y resultado no comparten nombre")
    void cada_uno_de_los_tres_desenlaces_produce_exactamente_su_terna() {
        assertThat(TrialPolicyOutcome.CONVERT_TO_PAID).satisfies(politica -> {
            assertThat(politica.chargeMode()).isEqualTo(TrialChargeMode.PAID);
            assertThat(politica.entitlementSource()).isEqualTo(TrialEntitlementSource.SUBSCRIPTION);
            assertThat(politica.accessLevel()).isEqualTo(TrialAccessLevel.FULL);
            assertThat(politica.resolvedOutcome()).isEqualTo(TrialOutcome.CONVERTED);
        });
        assertThat(TrialPolicyOutcome.LIMITED).satisfies(politica -> {
            assertThat(politica.chargeMode()).isEqualTo(TrialChargeMode.FREE_LIMITED);
            assertThat(politica.entitlementSource()).isEqualTo(TrialEntitlementSource.FREE_LIMITED);
            assertThat(politica.accessLevel()).isEqualTo(TrialAccessLevel.FULL);
            assertThat(politica.resolvedOutcome()).isEqualTo(TrialOutcome.LIMITED);
        });
        assertThat(TrialPolicyOutcome.READ_ONLY).satisfies(politica -> {
            assertThat(politica.chargeMode()).isEqualTo(TrialChargeMode.EXPIRED_READ_ONLY);
            assertThat(politica.entitlementSource())
                    .isEqualTo(TrialEntitlementSource.EXPIRED_TRIAL);
            assertThat(politica.accessLevel()).isEqualTo(TrialAccessLevel.READ_ONLY);
            assertThat(politica.resolvedOutcome()).isEqualTo(TrialOutcome.READ_ONLY);
        });
    }

    @ParameterizedTest
    @EnumSource(TrialPolicyOutcome.class)
    @DisplayName("ninguna política se queda sin terna: el switch cubre las tres ramas")
    void ninguna_politica_se_queda_sin_terna(TrialPolicyOutcome politica) {
        assertThat(politica.chargeMode()).isNotNull();
        assertThat(politica.entitlementSource()).isNotNull();
        assertThat(politica.accessLevel()).isNotNull();
        assertThat(politica.resolvedOutcome()).isNotNull();
    }

    @Test
    @DisplayName("R-TRIAL-14 · una línea TRIAL o FREE_LIMITED jamás genera un cargo; solo PAID")
    void una_linea_TRIAL_o_FREE_LIMITED_jamas_genera_un_cargo() {
        assertThat(TrialChargeMode.TRIAL.generatesCharge()).isFalse();
        assertThat(TrialChargeMode.FREE_LIMITED.generatesCharge()).isFalse();
        assertThat(TrialChargeMode.EXPIRED_READ_ONLY.generatesCharge()).isFalse();
        assertThat(TrialChargeMode.PAID.generatesCharge()).isTrue();
    }

    /**
     * <strong>El documento de diseño se equivoca en la cifra, y da igual.</strong>
     * Dice que {@code EXPIRED_READ_ONLY} tiene dieciocho caracteres y tiene
     * <em>diecisiete</em>. Lo que sostiene la decisión no es el número exacto sino
     * las dos desigualdades: no cabe en {@code VARCHAR(15)} y sí en
     * {@code VARCHAR(20)}. Por eso la prueba afirma eso y no la cuenta — una prueba
     * que copia una cifra mal leída se rompe cuando alguien la corrige, que es
     * justo al revés de lo que hace falta.
     */
    @Test
    @DisplayName("R-TRIAL-08 · el desenlace de solo lectura no cabe en VARCHAR(15) y sí en"
            + " VARCHAR(20): con quince, el barrido nocturno fallaría sobre todos los clientes"
            + " a la vez")
    void con_VARCHAR_15_el_barrido_nocturno_falla_sobre_todos_los_clientes_a_la_vez() {
        int ancho = TrialChargeMode.EXPIRED_READ_ONLY.name().length();

        assertThat(ancho).isGreaterThan(15).isLessThanOrEqualTo(20);
        assertThat(TrialChargeMode.values())
                .allSatisfy(modo -> assertThat(modo.name().length()).isLessThanOrEqualTo(20));
    }
}
