package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Los dos mapeadores son el único sitio que conoce a la vez el dominio y la
 * fila. Un campo que se pierda aquí no rompe nada visible: se guarda un
 * {@code null} y el defecto aparece al leer, ya en producción y sin rastro de
 * cuándo se escribió mal.
 *
 * <p>
 * Dos conversiones concretas justifican el archivo por sí solas: el
 * {@code int}↔{@code short} de {@code maxAttempts} —la columna es
 * {@code SMALLINT}— y el enum de decisión, que en la tabla es texto porque
 * {@code ENUM} de MySQL obligaría a un {@code ALTER} para añadir un valor.
 */
@DisplayName("Los mapeadores de platformaccess — ida y vuelta sin perder campos")
class PlatformAccessJpaMappersTest {

    @Nested
    @DisplayName("PlatformAccessRequestJpaMapper")
    class SolicitudMapper {

        private final PlatformAccessRequestJpaMapper mapper = new PlatformAccessRequestJpaMapper();

        @Test
        @DisplayName("una solicitud pendiente sobrevive a la ida y vuelta campo por campo")
        void una_solicitud_pendiente_sobrevive_a_la_ida_y_vuelta() {
            PlatformAccessRequest original = PlatformAccessMother.solicitudPendiente();

            PlatformAccessRequest vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getFullName()).isEqualTo(original.getFullName());
            assertThat(vuelta.getEmail()).isEqualTo(original.getEmail());
            assertThat(vuelta.getReason()).isEqualTo(original.getReason());
            assertThat(vuelta.getApprovalTokenHash()).isEqualTo(original.getApprovalTokenHash());
            assertThat(vuelta.getVerificationCodeHash())
                    .isEqualTo(original.getVerificationCodeHash());
            assertThat(vuelta.getVerificationAttempts())
                    .isEqualTo(original.getVerificationAttempts());
            assertThat(vuelta.getMaxAttempts()).isEqualTo(original.getMaxAttempts());
            assertThat(vuelta.getExpiresAt()).isEqualTo(original.getExpiresAt());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
            assertThat(vuelta.getDecision()).isNull();
            assertThat(vuelta.getDecidedAt()).isNull();
        }

        @Test
        @DisplayName("la decision viaja como texto a la columna y vuelve como enum")
        void la_decision_viaja_como_texto() {
            PlatformAccessRequest rechazada = PlatformAccessMother
                    .solicitudDecidida(PlatformAccessDecision.REJECTED);

            PlatformAccessRequestJpaEntity fila = mapper.toJpa(rechazada);

            // La columna es VARCHAR(10) con CHECK, no un ENUM de MySQL: anadir un
            // valor no puede exigir un ALTER de tabla.
            assertThat(fila.getDecision()).isEqualTo("REJECTED");
            assertThat(mapper.toDomain(fila).getDecision())
                    .isEqualTo(PlatformAccessDecision.REJECTED);
            assertThat(mapper.toDomain(fila).getDecidedAt()).isEqualTo(rechazada.getDecidedAt());
        }

        @Test
        @DisplayName("una solicitud sin decidir escribe NULL, no la cadena \"null\"")
        void una_solicitud_sin_decidir_escribe_null() {
            assertThat(mapper.toJpa(PlatformAccessMother.solicitudPendiente()).getDecision())
                    .isNull();
        }

        @Test
        @DisplayName("maxAttempts baja a short sin perder el 5 de la politica")
        void max_attempts_baja_a_short_sin_perder_valor() {
            PlatformAccessRequestJpaEntity fila = mapper
                    .toJpa(PlatformAccessMother.solicitudPendiente());

            assertThat(fila.getMaxAttempts()).isEqualTo((short) 5);
            assertThat(mapper.toDomain(fila).getMaxAttempts()).isEqualTo(5);
        }

        @Test
        @DisplayName("el contador de intentos gastados vuelve intacto: es lo que decide el 429")
        void el_contador_de_intentos_vuelve_intacto() {
            PlatformAccessRequest conCuatro = PlatformAccessMother.solicitud(4, null, null,
                    PlatformAccessMother.AHORA.plusHours(1));

            PlatformAccessRequest vuelta = mapper.toDomain(mapper.toJpa(conCuatro));

            assertThat(vuelta.getVerificationAttempts()).isEqualTo(4);
            assertThat(vuelta.remainingAttempts()).isEqualTo(1);
            assertThat(vuelta.isBlocked()).isFalse();
        }

        @Test
        @DisplayName("una fila con una decision que el CHECK no vio revienta al leerse")
        void una_fila_con_decision_desconocida_revienta_al_leerse() {
            PlatformAccessRequestJpaEntity fila = mapper
                    .toJpa(PlatformAccessMother.solicitudPendiente());
            fila.setDecision("MAYBE");
            fila.setDecidedAt(LocalDateTime.now(PlatformAccessMother.RELOJ));

            // Degradar a null convertiria una solicitud resuelta en pendiente y
            // permitiria decidirla otra vez.
            assertThatThrownBy(() -> mapper.toDomain(fila))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown platform access decision");
        }
    }

    @Nested
    @DisplayName("PlatformAccessInvitationJpaMapper")
    class InvitacionMapper {

        private final PlatformAccessInvitationJpaMapper mapper = new PlatformAccessInvitationJpaMapper();

        @Test
        @DisplayName("una invitacion viva sobrevive a la ida y vuelta campo por campo")
        void una_invitacion_viva_sobrevive_a_la_ida_y_vuelta() {
            PlatformAccessInvitation original = PlatformAccessMother.invitacionViva();

            PlatformAccessInvitation vuelta = mapper.toDomain(mapper.toJpa(original));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getAccessRequestId()).isEqualTo(original.getAccessRequestId());
            assertThat(vuelta.getTokenHash()).isEqualTo(original.getTokenHash());
            assertThat(vuelta.getExpiresAt()).isEqualTo(original.getExpiresAt());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.getConsumedAt()).isNull();
            assertThat(vuelta.getSystemUserId()).isNull();
        }

        @Test
        @DisplayName("el par consumo/usuario vuelve entero: es la trazabilidad del alta")
        void el_par_consumo_usuario_vuelve_entero() {
            PlatformAccessInvitation consumida = PlatformAccessMother.invitacionConsumida();

            PlatformAccessInvitation vuelta = mapper.toDomain(mapper.toJpa(consumida));

            // Sin systemUserId no se puede demostrar que cuenta salio de que
            // invitacion, que es la evidencia que el rastro durable debe conservar.
            assertThat(vuelta.getConsumedAt()).isEqualTo(consumida.getConsumedAt());
            assertThat(vuelta.getSystemUserId()).isEqualTo(consumida.getSystemUserId());
            assertThat(vuelta.isConsumed()).isTrue();
        }

        @Test
        @DisplayName("una invitacion sin id todavia mapea: el id lo pone la base al insertar")
        void una_invitacion_sin_id_todavia_mapea() {
            PlatformAccessInvitation nueva = PlatformAccessInvitation.issue(4271L,
                    PlatformAccessMother.hashDe("nueva"), PlatformAccessMother.AHORA,
                    PlatformAccessMother.AHORA.plusDays(7));

            assertThat(mapper.toJpa(nueva).getId()).isNull();
        }
    }
}
