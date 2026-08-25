package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformaccess.application.dto.PlatformAccessRequestDto;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.ApprovalResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La pantalla que ve el aprobador antes de decidir. Es un GET anónimo con el
 * token en la query, así que <b>todo lo que devuelva es público para quien
 * tenga el enlace</b>: por eso el DTO lleva cuatro campos y ninguno es un hash.
 *
 * <p>
 * Lo otro que se fija aquí es que los cinco estados no decidibles —inexistente,
 * caducado, decidido, bloqueado— salgan con <b>una sola</b> excepción y que el
 * motivo real viaje únicamente al canal de auditoría, que no sale al cliente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatePlatformAccessTokenService — la pantalla previa a decidir")
class ValidatePlatformAccessTokenServiceTest {

    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    private ValidatePlatformAccessTokenService crearServicio() {
        return new ValidatePlatformAccessTokenService(requestRepository, audit, metrics,
                PlatformAccessMother.RELOJ);
    }

    private void dadoQueElTokenResuelve(PlatformAccessRequest request) {
        when(requestRepository.findByApprovalTokenHash(anyString()))
                .thenReturn(Optional.of(request));
    }

    @Nested
    @DisplayName("solicitud pendiente")
    class Pendiente {

        @Test
        @DisplayName("devuelve los cuatro campos de la solicitud y ningun hash")
        void devuelve_los_cuatro_campos() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudPendiente());

            PlatformAccessRequestDto dto = crearServicio()
                    .execute(PlatformAccessMother.TOKEN_PLANO);

            assertThat(dto.fullName()).isEqualTo(PlatformAccessMother.NOMBRE);
            assertThat(dto.email()).isEqualTo(PlatformAccessMother.CORREO);
            assertThat(dto.reason()).isEqualTo(PlatformAccessMother.MOTIVO);
            assertThat(dto.requestedAt()).isEqualTo(PlatformAccessMother.AHORA.minusHours(1));
        }

        @Test
        @DisplayName("no gasta intento ni emite decision: solo mira")
        void no_gasta_intento_ni_decide() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudPendiente());

            crearServicio().execute(PlatformAccessMother.TOKEN_PLANO);

            // Con STRICT_STUBS, cualquier llamada a registerFailedAttempt o
            // applyDecision sin stub devolveria 0 en silencio: el verify es lo que
            // impide que un futuro refactor cuele una escritura en un GET.
            verify(requestRepository).findByApprovalTokenHash(anyString());
            verify(audit).bindRequest(PlatformAccessMother.ID_SOLICITUD);
            verify(audit).unbindRequest();
        }
    }

    @Nested
    @DisplayName("estados no decidibles — un solo codigo hacia fuera, el motivo solo al log")
    class NoDecidible {

        @Test
        @DisplayName("un token inexistente sale como token_invalid sin id de solicitud")
        void un_token_inexistente_sale_como_token_invalid() {
            when(requestRepository.findByApprovalTokenHash(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(audit).approvalDenied("token_invalid", null);
            verify(metrics).resolved(ApprovalResult.TOKEN_INVALID);
        }

        @Test
        @DisplayName("un token en blanco no llega a consultar la base")
        void un_token_en_blanco_no_consulta_la_base() {
            assertThatThrownBy(() -> crearServicio().execute("  "))
                    .isInstanceOf(InvalidApprovalTokenException.class)
                    .hasMessageContaining("Approval token is required");

            verify(audit).approvalDenied("token_invalid", null);
        }

        @Test
        @DisplayName("una solicitud caducada registra token_expired con su id")
        void una_solicitud_caducada_registra_token_expired() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudCaducada());

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class)
                    .hasMessageContaining("no longer resolvable");

            verify(audit).approvalDenied("token_expired", PlatformAccessMother.ID_SOLICITUD);
            verify(metrics).resolved(ApprovalResult.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("una solicitud ya decidida registra token_consumed")
        void una_solicitud_decidida_registra_token_consumed() {
            dadoQueElTokenResuelve(
                    PlatformAccessMother.solicitudDecidida(PlatformAccessDecision.APPROVED));

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(audit).approvalDenied("token_consumed", PlatformAccessMother.ID_SOLICITUD);
            verify(metrics).resolved(ApprovalResult.TOKEN_CONSUMED);
        }

        @Test
        @DisplayName("el bloqueo gana a todo: una solicitud bloqueada registra attempts_exhausted")
        void el_bloqueo_gana_a_todo() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudBloqueada());

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(audit).approvalDenied("attempts_exhausted", PlatformAccessMother.ID_SOLICITUD);
            verify(metrics).resolved(ApprovalResult.ATTEMPTS_EXHAUSTED);
        }

        @Test
        @DisplayName("bloqueada Y caducada a la vez sigue registrandose como bloqueo")
        void bloqueada_y_caducada_sigue_siendo_bloqueo() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitud(5, null, null,
                    PlatformAccessMother.AHORA.minusMinutes(1)));

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            // La precedencia BLOCKED > EXPIRED > PENDING no es cosmetica: el front
            // evalua el 429 antes que el 422 y si degradara volveria a ofrecer el
            // formulario del codigo a alguien que ya agoto los intentos.
            verify(audit).approvalDenied("attempts_exhausted", PlatformAccessMother.ID_SOLICITUD);
        }

        @Test
        @DisplayName("desata el MDC tambien cuando el estado hace fallar el caso de uso")
        void desata_el_mdc_en_el_camino_de_fallo() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudCaducada());

            assertThatThrownBy(() -> crearServicio().execute(PlatformAccessMother.TOKEN_PLANO))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(audit).bindRequest(PlatformAccessMother.ID_SOLICITUD);
            verify(audit).unbindRequest();
        }
    }
}
