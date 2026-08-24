package com.vetsoftware.app.dunning.domain;

import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.AHORA;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.EMPRESA;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.contrato;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.contratoDeOtraEmpresa;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.factura;
import static com.vetsoftware.app.dunning.testsupport.DunningEventMother.facturaDeOtraEmpresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DunningEventTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("un recordatorio queda anotado con su canal, sus dias de mora y su detalle")
        void anota_el_recordatorio() {
            DunningEvent event = DunningEvent.record(EMPRESA, contrato(), factura(),
                    DunningEventType.REMINDER_SENT, 5, DunningChannel.EMAIL, "Primer aviso", AHORA,
                    AHORA);

            assertThat(event.getEventType()).isEqualTo(DunningEventType.REMINDER_SENT);
            assertThat(event.getChannel()).isEqualTo(DunningChannel.EMAIL);
            assertThat(event.getDaysOverdue()).isEqualTo(5);
            assertThat(event.getDetail()).isEqualTo("Primer aviso");
            assertThat(event.getBillingDocument().documentNumber()).isEqualTo("FAC-2026-0001");
        }

        @Test
        @DisplayName("un evento de contrato no cuelga de ninguna factura")
        void evento_de_contrato_sin_factura() {
            DunningEvent event = DunningEvent.record(EMPRESA, contrato(), null,
                    DunningEventType.READ_ONLY_APPLIED, 30, null, null, AHORA, AHORA);

            assertThat(event.getBillingDocument()).isNull();
            assertThat(event.getChannel()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un recordatorio sin canal no prueba nada y se rechaza")
        void recordatorio_exige_canal() {
            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, contrato(), factura(),
                    DunningEventType.REMINDER_SENT, 5, null, null, AHORA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("channel is required for a REMINDER_SENT");
        }

        @ParameterizedTest
        @EnumSource(value = DunningEventType.class, mode = EnumSource.Mode.EXCLUDE, names = "REMINDER_SENT")
        @DisplayName("los demas hitos no exigen canal: no son un aviso al cliente")
        void los_demas_no_exigen_canal(DunningEventType eventType) {
            assertThatCode(() -> DunningEvent.record(EMPRESA, contrato(), null, eventType, 30, null,
                    null, AHORA, AHORA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza dias de mora negativos")
        void rechaza_dias_negativos() {
            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, contrato(), null,
                    DunningEventType.GRACE_STARTED, -1, null, null, AHORA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("daysOverdue cannot be negative");
        }

        @Test
        @DisplayName("rechaza un detalle que no cabe en la columna")
        void rechaza_detalle_largo() {
            String detalle = "x".repeat(256);

            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, contrato(), null,
                    DunningEventType.GRACE_STARTED, 1, null, detalle, AHORA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("255 chars or less");
        }

        @Test
        @DisplayName("un evento sin contrato no ata a nada y se rechaza")
        void exige_contrato() {
            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, null, null,
                    DunningEventType.GRACE_STARTED, 1, null, null, AHORA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subscription is required");
        }

        @Test
        @DisplayName("un evento sin fecha de ocurrencia no sirve como prueba")
        void exige_fecha() {
            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, contrato(), null,
                    DunningEventType.GRACE_STARTED, 1, null, null, null, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("occurredAt is required");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un evento propio no puede colgar del contrato de otra clinica")
        void no_cuelga_del_contrato_ajeno() {
            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, contratoDeOtraEmpresa(), null,
                    DunningEventType.GRACE_STARTED, 1, null, null, AHORA, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subscription belongs to another company");
        }

        @Test
        @DisplayName("un evento propio no puede citar la factura de otra clinica")
        void no_cita_la_factura_ajena() {
            assertThatThrownBy(() -> DunningEvent.record(EMPRESA, contrato(),
                    facturaDeOtraEmpresa(), DunningEventType.REMINDER_SENT, 1, DunningChannel.SMS,
                    null, AHORA, AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billing document belongs to another company");
        }
    }

    @Nested
    @DisplayName("Politica R18 - no existe el corte total de acceso")
    class PoliticaDeCorte {

        @Test
        @DisplayName("READ_ONLY_APPLIED es el hito mas restrictivo del vocabulario")
        void no_hay_hito_de_bloqueo() {
            assertThat(DunningEventType.values()).containsExactly(DunningEventType.REMINDER_SENT,
                    DunningEventType.GRACE_STARTED, DunningEventType.READ_ONLY_APPLIED,
                    DunningEventType.REACTIVATED, DunningEventType.WRITTEN_OFF);
        }

        @Test
        @DisplayName("ningun hito nombra un bloqueo, una suspension ni un corte")
        void ningun_hito_nombra_un_corte() {
            assertThat(DunningEventType.values()).extracting(Enum::name)
                    .noneMatch(name -> name.contains("BLOCK") || name.contains("SUSPEND")
                            || name.contains("CUT") || name.contains("DISABLE"));
        }
    }
}
