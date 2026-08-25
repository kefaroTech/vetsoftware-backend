package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.ApprovalResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessBlockedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessCodeMismatchException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Rechazar es la otra mitad de la decisión, y el punto entero de este archivo
 * es que <b>no se puede relajar respecto a aprobar</b>.
 *
 * <p>
 * Quien puede rechazar puede aprobar: es el mismo token y el mismo código, no
 * hay uno por decisión. Si el rechazo comprobara menos —si no gastara intento,
 * si aceptara un enlace ya usado, si decidiera con un {@code UPDATE}
 * incondicional— sería un canal para quemar solicitudes ajenas con menos
 * credencial de la que hace falta para aprobarlas. Y el efecto de quemar una
 * solicitud es terminal: no hay reintento, la persona tiene que volver a pedir
 * acceso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RejectPlatformAccessRequestService — rechazar exige lo mismo que aprobar")
class RejectPlatformAccessRequestServiceTest {

    private static final LocalDateTime AHORA = PlatformAccessMother.AHORA;

    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private SecretHasherPort secretHasher;
    @Mock
    private PlatformAccessEmailSender emailSender;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    private RejectPlatformAccessRequestService crearServicio() {
        return new RejectPlatformAccessRequestService(requestRepository, secretHasher, emailSender,
                audit, metrics, PlatformAccessMother.RELOJ);
    }

    private void dadoQueElTokenResuelve(PlatformAccessRequest request) {
        when(requestRepository.findByApprovalTokenHash(anyString()))
                .thenReturn(Optional.of(request));
    }

    private static ResolvePlatformAccessCommand comando(String codigo) {
        return new ResolvePlatformAccessCommand(PlatformAccessMother.TOKEN_PLANO, codigo);
    }

    @Nested
    @DisplayName("camino feliz")
    class Rechazo {

        @Test
        @DisplayName("marca REJECTED con la hora del reloj inyectado y avisa al solicitante")
        void marca_rejected_y_avisa_al_solicitante() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(eq(PlatformAccessMother.CODIGO),
                    eq(PlatformAccessMother.HASH_CODIGO))).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);

            crearServicio().execute(comando(PlatformAccessMother.CODIGO));

            ArgumentCaptor<PlatformAccessDecision> decision = ArgumentCaptor
                    .forClass(PlatformAccessDecision.class);
            ArgumentCaptor<LocalDateTime> decidida = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(requestRepository).applyDecision(eq(PlatformAccessMother.ID_SOLICITUD),
                    decision.capture(), decidida.capture());
            // Sin capturar el valor, un rechazo que escribiera APPROVED pasaria igual.
            assertThat(decision.getValue()).isEqualTo(PlatformAccessDecision.REJECTED);
            assertThat(decidida.getValue()).isEqualTo(AHORA);

            verify(audit).requestRejected(PlatformAccessMother.ID_SOLICITUD);
            verify(metrics).resolved(ApprovalResult.REJECTED);
            verify(emailSender).sendRejection(PlatformAccessMother.ID_SOLICITUD,
                    PlatformAccessMother.CORREO, PlatformAccessMother.NOMBRE);
        }

        @Test
        @DisplayName("el aviso de rechazo NO lleva motivo: solo id, correo y nombre")
        void el_aviso_no_lleva_motivo() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);

            crearServicio().execute(comando(PlatformAccessMother.CODIGO));

            // Explicar el rechazo convertiria el correo en un canal para deducir que
            // criterios usa quien aprueba. La firma del puerto es la garantia: si
            // alguien anadiera el motivo, este test deja de compilar.
            verify(emailSender).sendRejection(PlatformAccessMother.ID_SOLICITUD,
                    PlatformAccessMother.CORREO, PlatformAccessMother.NOMBRE);
        }

        @Test
        @DisplayName("desata el MDC del id de solicitud al terminar bien")
        void desata_el_mdc_al_terminar_bien() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);

            crearServicio().execute(comando(PlatformAccessMother.CODIGO));

            verify(audit).bindRequest(PlatformAccessMother.ID_SOLICITUD);
            verify(audit).unbindRequest();
        }
    }

    @Nested
    @DisplayName("no emite invitacion jamas")
    class SinInvitacion {

        @Test
        @DisplayName("el rechazo no toca el repositorio de invitaciones: no existe en el constructor")
        void el_rechazo_no_conoce_las_invitaciones() {
            // La garantia estructural, no una asercion de comportamiento: el servicio
            // no recibe PlatformAccessInvitationRepository, asi que no hay camino por
            // el que un rechazo pueda emitir la credencial de alta.
            assertThat(RejectPlatformAccessRequestService.class.getDeclaredConstructors()[0]
                    .getParameterTypes()).doesNotContain(
                            com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository.class);
        }
    }

    @Nested
    @DisplayName("token muerto")
    class TokenMuerto {

        @Test
        @DisplayName("un token inexistente no gasta intento ni manda correo")
        void un_token_inexistente_no_gasta_intento() {
            when(requestRepository.findByApprovalTokenHash(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> crearServicio().execute(comando(PlatformAccessMother.CODIGO)))
                    .isInstanceOf(InvalidApprovalTokenException.class)
                    .hasMessageContaining("does not exist");

            verify(audit).approvalDenied("token_invalid", null);
            verify(metrics).resolved(ApprovalResult.TOKEN_INVALID);
            verify(requestRepository, never()).registerFailedAttempt(anyLong());
            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("un token en blanco ni siquiera consulta la base")
        void un_token_en_blanco_no_consulta_la_base() {
            assertThatThrownBy(() -> crearServicio()
                    .execute(new ResolvePlatformAccessCommand("   ", PlatformAccessMother.CODIGO)))
                    .isInstanceOf(InvalidApprovalTokenException.class)
                    .hasMessageContaining("Approval token is required");

            verifyNoInteractions(emailSender);
            verify(requestRepository, never()).findByApprovalTokenHash(anyString());
        }

        @Test
        @DisplayName("una solicitud YA rechazada se denuncia como reproduccion, no como caducidad")
        void una_solicitud_ya_rechazada_es_reproduccion() {
            dadoQueElTokenResuelve(
                    PlatformAccessMother.solicitudDecidida(PlatformAccessDecision.REJECTED));

            assertThatThrownBy(() -> crearServicio().execute(comando(PlatformAccessMother.CODIGO)))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            // 1800 s = los 30 minutos del mother. Separan el doble clic de quien
            // reproduce un correo filtrado, que es por lo que el evento es WARN.
            verify(audit).approvalDeniedByReplay(PlatformAccessMother.ID_SOLICITUD, 1800L);
            verify(metrics).resolved(ApprovalResult.TOKEN_CONSUMED);
            verify(secretHasher, never()).matches(anyString(), anyString());
            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("una solicitud ya APROBADA tampoco se puede rechazar despues")
        void una_solicitud_ya_aprobada_no_se_puede_rechazar() {
            dadoQueElTokenResuelve(
                    PlatformAccessMother.solicitudDecidida(PlatformAccessDecision.APPROVED));

            // Si el rechazo pudiera pisar una aprobacion, la invitacion ya emitida
            // seguiria viva y la fila diria lo contrario.
            assertThatThrownBy(() -> crearServicio().execute(comando(PlatformAccessMother.CODIGO)))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(requestRepository, never()).applyDecision(anyLong(), any(), any());
        }

        @Test
        @DisplayName("una solicitud caducada no llega a comprobar el codigo")
        void una_solicitud_caducada_no_comprueba_el_codigo() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudCaducada());

            assertThatThrownBy(() -> crearServicio().execute(comando(PlatformAccessMother.CODIGO)))
                    .isInstanceOf(InvalidApprovalTokenException.class)
                    .hasMessageContaining("expired");

            verify(audit).approvalDenied("token_expired", PlatformAccessMother.ID_SOLICITUD);
            verify(metrics).resolved(ApprovalResult.TOKEN_EXPIRED);
            verify(secretHasher, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("el codigo gasta intentos igual que al aprobar")
    class CodigoDeVerificacion {

        @Test
        @DisplayName("un codigo incorrecto gasta un intento y sale 422 con el margen restante")
        void un_codigo_incorrecto_gasta_un_intento() {
            dadoQueElTokenResuelve(
                    PlatformAccessMother.solicitud(1, null, null, AHORA.plusHours(1)));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(false);
            when(requestRepository.registerFailedAttempt(PlatformAccessMother.ID_SOLICITUD))
                    .thenReturn(1);

            assertThatThrownBy(() -> crearServicio().execute(comando("000000")))
                    .isInstanceOf(PlatformAccessCodeMismatchException.class)
                    .extracting(
                            e -> ((PlatformAccessCodeMismatchException) e).getRemainingAttempts())
                    .isEqualTo(3);

            verify(audit).approvalDeniedByCodeMismatch(PlatformAccessMother.ID_SOLICITUD, 3);
            verify(metrics).resolved(ApprovalResult.CODE_MISMATCH);
            verify(requestRepository, never()).applyDecision(anyLong(), any(), any());
            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("el ultimo intento sale 429, no 422 con cero")
        void el_ultimo_intento_bloquea() {
            dadoQueElTokenResuelve(
                    PlatformAccessMother.solicitud(4, null, null, AHORA.plusHours(1)));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(false);
            when(requestRepository.registerFailedAttempt(PlatformAccessMother.ID_SOLICITUD))
                    .thenReturn(1);

            assertThatThrownBy(() -> crearServicio().execute(comando("000000")))
                    .isInstanceOf(PlatformAccessBlockedException.class);

            verify(audit).approvalLocked(PlatformAccessMother.ID_SOLICITUD);
            verify(metrics).resolved(ApprovalResult.ATTEMPTS_EXHAUSTED);
            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("una solicitud ya bloqueada sale 429 sin gastar otro intento")
        void una_solicitud_ya_bloqueada_no_gasta_otro_intento() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudBloqueada());

            assertThatThrownBy(() -> crearServicio().execute(comando(PlatformAccessMother.CODIGO)))
                    .isInstanceOf(PlatformAccessBlockedException.class)
                    .hasMessageContaining("permanently blocked");

            verify(requestRepository, never()).registerFailedAttempt(anyLong());
            verify(secretHasher, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("un codigo nulo se compara como cadena vacia, no revienta con NPE")
        void un_codigo_nulo_se_compara_como_vacio() {
            dadoQueElTokenResuelve(
                    PlatformAccessMother.solicitud(1, null, null, AHORA.plusHours(1)));
            when(secretHasher.matches(eq(""), anyString())).thenReturn(false);
            when(requestRepository.registerFailedAttempt(anyLong())).thenReturn(1);

            assertThatThrownBy(() -> crearServicio().execute(
                    new ResolvePlatformAccessCommand(PlatformAccessMother.TOKEN_PLANO, null)))
                    .isInstanceOf(PlatformAccessCodeMismatchException.class);

            // Gasta intento igual: un NPE saldria como 500 y no consumiria nada, que
            // es una via para probar codigos gratis.
            verify(requestRepository).registerFailedAttempt(PlatformAccessMother.ID_SOLICITUD);
        }
    }

    @Nested
    @DisplayName("carrera contra la otra pestana")
    class Carrera {

        @Test
        @DisplayName("si el UPDATE no afecta filas, no se avisa al solicitante")
        void si_el_update_no_afecta_filas_no_se_avisa() {
            dadoQueElTokenResuelve(PlatformAccessMother.solicitudPendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(0);

            // Entre la lectura y el UPDATE otra pestana pudo aprobar: el rowcount es
            // lo que decide, no el if de arriba.
            assertThatThrownBy(() -> crearServicio().execute(comando(PlatformAccessMother.CODIGO)))
                    .isInstanceOf(InvalidApprovalTokenException.class)
                    .hasMessageContaining("no longer resolvable");

            verify(audit).approvalDeniedByReplay(PlatformAccessMother.ID_SOLICITUD, 0L);
            verify(metrics).resolved(ApprovalResult.TOKEN_CONSUMED);
            verify(audit).unbindRequest();
            verifyNoInteractions(emailSender);
        }
    }
}
