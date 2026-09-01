package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * ⛔ <b>La partición que decide quién paga.</b> Se afirma sobre el enum
 * <em>entero y por exhaustividad</em>, no sobre una muestra: un desenlace nuevo
 * cae por defecto en «no se invocó» —o sea, cupo devuelto— y ese es justo el
 * lado en el que un olvido no se nota.
 */
@DisplayName("GenerationOutcome — qué desenlaces costaron dinero")
class GenerationOutcomeTest {

    @ParameterizedTest
    @EnumSource(value = GenerationOutcome.class, names = {"SUCCEEDED", "MODEL_FAILED"})
    @DisplayName("se invocó al modelo, se pagó: incluida la invocación que falló")
    void los_dos_desenlaces_que_costaron_dinero(GenerationOutcome outcome) {
        assertThat(outcome.huboInvocacionDePago()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = GenerationOutcome.class, names = {"DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS",
            "DEGRADED_MODEL_UNAVAILABLE"})
    @DisplayName("las tres degradaciones se deciden antes de llamar: no costaron nada")
    void las_tres_degradaciones_no_costaron_nada(GenerationOutcome outcome) {
        assertThat(outcome.huboInvocacionDePago()).isFalse();
    }

    /**
     * Sin esto, añadir un sexto valor al enum dejaría las dos pruebas de arriba en
     * verde sin haber decidido de qué lado cae — y el lado por defecto es el que
     * regala cupo.
     */
    @Test
    @DisplayName("los cinco desenlaces del dominio están clasificados: no hay ninguno sin decidir")
    void los_cinco_desenlaces_estan_clasificados() {
        assertThat(Arrays.stream(GenerationOutcome.values())
                .filter(GenerationOutcome::huboInvocacionDePago).toList())
                .containsExactlyInAnyOrder(GenerationOutcome.SUCCEEDED,
                        GenerationOutcome.MODEL_FAILED);
        assertThat(GenerationOutcome.values()).hasSize(5);
    }
}
