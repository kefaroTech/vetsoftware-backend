package com.vetsoftware.app.companyentitlementsnapshot.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("CompanyEntitlementSnapshot — la foto de cada recálculo")
class CompanyEntitlementSnapshotTest {

    private static final Long ANA = 42L;
    private static final LocalDateTime TRES_DE_MARZO = LocalDateTime.of(2026, 3, 3, 11, 0);
    private static final String PAYLOAD = "{\"entitlements\":[{\"subModule\":\"AGENDA\"}]}";

    @Nested
    @DisplayName("la evidencia que el recálculo destruiría")
    class Evidencia {

        @Test
        @DisplayName("guarda qué veía la empresa el 3 de marzo, con su versión de formato fuera"
                + " del documento")
        void guarda_que_veia_la_empresa_el_3_de_marzo() {
            CompanyEntitlementSnapshot foto = CompanyEntitlementSnapshot.take(ANA, TRES_DE_MARZO,
                    SnapshotActor.automatedProcess(), SnapshotTriggerReason.TRIAL_EXPIRED, null,
                    PAYLOAD, 1);

            assertThat(foto.getPayload()).isEqualTo(PAYLOAD);
            assertThat(foto.getPayloadFormatVersion()).isEqualTo(1);
            assertThat(foto.getRecalculatedAt()).isEqualTo(TRES_DE_MARZO);
        }

        @Test
        @DisplayName("no expone ningún mutador: una foto retocada no demuestra nada")
        void no_expone_ningun_mutador() {
            assertThat(Arrays.stream(CompanyEntitlementSnapshot.class.getMethods())
                    .map(Method::getName)).noneMatch(nombre -> nombre.startsWith("set"));
        }

        @Test
        @DisplayName("una foto vacía se rechaza")
        void una_foto_vacia_se_rechaza() {
            assertThatThrownBy(() -> CompanyEntitlementSnapshot.take(ANA, TRES_DE_MARZO,
                    SnapshotActor.automatedProcess(), SnapshotTriggerReason.MANUAL, null, "  ", 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("proves nothing");
        }

        @Test
        @DisplayName("una versión de formato menor que uno se rechaza")
        void una_version_de_formato_menor_que_uno_se_rechaza() {
            assertThatThrownBy(() -> CompanyEntitlementSnapshot.take(ANA, TRES_DE_MARZO,
                    SnapshotActor.automatedProcess(), SnapshotTriggerReason.MANUAL, null, PAYLOAD,
                    0)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least 1");
        }
    }

    @Nested
    @DisplayName("el otrosí es el único motivo que nombra un papel")
    class Motivo {

        @Test
        @DisplayName("una foto de otrosí sin el otrosí se rechaza")
        void una_foto_de_otrosi_sin_el_otrosi_se_rechaza() {
            assertThatThrownBy(() -> CompanyEntitlementSnapshot.take(ANA, TRES_DE_MARZO,
                    SnapshotActor.automatedProcess(), SnapshotTriggerReason.CONTRACT_AMENDMENT,
                    null, PAYLOAD, 1)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must name the amendment");
        }

        @ParameterizedTest
        @EnumSource(value = SnapshotTriggerReason.class, names = {"TRIAL_EXPIRED", "DUNNING",
                "MANUAL", "REPAIR"})
        @DisplayName("los demás motivos no necesitan papel")
        void los_demas_motivos_no_necesitan_papel(SnapshotTriggerReason motivo) {
            CompanyEntitlementSnapshot foto = CompanyEntitlementSnapshot.take(ANA, TRES_DE_MARZO,
                    SnapshotActor.automatedProcess(), motivo, null, PAYLOAD, 1);

            assertThat(foto.getTriggerReason()).isEqualTo(motivo);
            assertThat(motivo.requiresAmendment()).isFalse();
        }
    }

    @Nested
    @DisplayName("el trío del actor: exactamente uno")
    class Actor {

        @Test
        @DisplayName("una foto sin actor se rechaza")
        void una_foto_sin_actor_se_rechaza() {
            assertThatThrownBy(() -> new SnapshotActor(null, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one actor");
        }

        @Test
        @DisplayName("una foto con empleado y proceso a la vez se rechaza")
        void una_foto_con_dos_actores_se_rechaza() {
            assertThatThrownBy(() -> new SnapshotActor(9L, null, true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("las tres factorías producen un actor válido")
        void las_tres_factorias_producen_un_actor_valido() {
            assertThat(SnapshotActor.employee(9L).employeeId()).isEqualTo(9L);
            assertThat(SnapshotActor.systemUser(3L).systemUserId()).isEqualTo(3L);
            assertThat(SnapshotActor.automatedProcess().process()).isTrue();
        }
    }
}
