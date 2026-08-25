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
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.ApprovalResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessBlockedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessCodeMismatchException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Aprobar es la mitad del flujo donde se decide quién acaba con control total
 * de la plataforma. Lo que se fija aquí es la <b>precedencia de estados</b> —
 * bloqueado gana a consumido, consumido a caducado— y que cada camino de fallo
 * gaste (o no) un intento del contador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovePlatformAccessRequestService — aprobar con token y código")
class ApprovePlatformAccessRequestServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"),
            ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(RELOJ.instant(),
            ZoneOffset.UTC);
    private static final String MOTIVO = "Necesito acceso para operar la plataforma";
    private static final String HASH_CODIGO = "$2a$10$hash-bcrypt";
    private static final String TOKEN_PLANO = "token-plano-de-prueba";

    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private PlatformAccessInvitationRepository invitationRepository;
    @Mock
    private SecretHasherPort secretHasher;
    @Mock
    private PlatformAccessEmailSender emailSender;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    private ApprovePlatformAccessRequestService crearServicio() {
        return new ApprovePlatformAccessRequestService(requestRepository, invitationRepository,
                secretHasher, emailSender, audit, metrics, RELOJ, 7L);
    }

    private static PlatformAccessRequest solicitud(int intentos, PlatformAccessDecision decision,
            LocalDateTime decidida, LocalDateTime expira) {
        return new PlatformAccessRequest(4271L, "Ana Ramirez", "ana@vetrina.co", MOTIVO,
                "a".repeat(64), HASH_CODIGO, intentos, 5, expira, decision, decidida,
                AHORA.minusHours(1), 0L);
    }

    private static PlatformAccessRequest pendiente() {
        return solicitud(0, null, null, AHORA.plusHours(1));
    }

    private void dadoQueElTokenResuelve(PlatformAccessRequest request) {
        when(requestRepository.findByApprovalTokenHash(anyString()))
                .thenReturn(Optional.of(request));
    }

    private static ResolvePlatformAccessCommand comando(String codigo) {
        return new ResolvePlatformAccessCommand(TOKEN_PLANO, codigo);
    }

    @Nested
    @DisplayName("camino feliz")
    class Aprobacion {

        @Test
        @DisplayName("aplica la decision, emite la invitacion y difiere el correo")
        void aplica_la_decision_y_emite_la_invitacion() {
            dadoQueElTokenResuelve(pendiente());
            when(secretHasher.matches(eq("123456"), eq(HASH_CODIGO))).thenReturn(true);
            when(requestRepository.applyDecision(eq(4271L), eq(PlatformAccessDecision.APPROVED),
                    any())).thenReturn(1);
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            crearServicio().execute(comando("123456"));

            ArgumentCaptor<PlatformAccessInvitation> guardada = ArgumentCaptor
                    .forClass(PlatformAccessInvitation.class);
            verify(invitationRepository).save(guardada.capture());
            assertThat(guardada.getValue().getAccessRequestId()).isEqualTo(4271L);
            assertThat(guardada.getValue().getExpiresAt()).isEqualTo(AHORA.plusDays(7));
            // En la base solo queda el hash; el valor plano solo existe en el correo.
            assertThat(guardada.getValue().getTokenHash()).hasSize(64);
            assertThat(guardada.getValue().getConsumedAt()).isNull();

            verify(audit).requestApproved(4271L);
            verify(metrics).resolved(ApprovalResult.APPROVED);
            verify(emailSender).sendInvitation(eq(4271L), eq("ana@vetrina.co"), eq("Ana Ramirez"),
                    anyString());
        }

        @Test
        @DisplayName("desata el MDC del id de solicitud aunque el caso de uso termine bien")
        void desata_el_mdc_al_terminar() {
            dadoQueElTokenResuelve(pendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            crearServicio().execute(comando("123456"));

            // Un put sin su remove en un pool de hilos etiqueta la peticion del
            // siguiente usuario con el id de una solicitud ajena.
            verify(audit).bindRequest(4271L);
            verify(audit).unbindRequest();
        }
    }

    @Nested
    @DisplayName("token muerto")
    class TokenMuerto {

        @Test
        @DisplayName("un token que no existe no gasta intento y no toca la invitacion")
        void un_token_que_no_existe_no_gasta_intento() {
            when(requestRepository.findByApprovalTokenHash(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> crearServicio().execute(comando("123456")))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(audit).approvalDenied("token_invalid", null);
            verify(metrics).resolved(ApprovalResult.TOKEN_INVALID);
            verify(requestRepository, never()).registerFailedAttempt(anyLong());
            verifyNoInteractions(invitationRepository, emailSender);
        }

        @Test
        @DisplayName("un token vacio se trata igual que uno inexistente, sin consultar la base")
        void un_token_vacio_se_trata_igual() {
            assertThatThrownBy(
                    () -> crearServicio().execute(new ResolvePlatformAccessCommand("  ", "123456")))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verifyNoInteractions(invitationRepository, emailSender);
        }

        @Test
        @DisplayName("una solicitud ya decidida emite el evento de reproduccion, no el de caducidad")
        void una_solicitud_ya_decidida_es_reproduccion() {
            dadoQueElTokenResuelve(solicitud(0, PlatformAccessDecision.APPROVED,
                    AHORA.minusMinutes(30), AHORA.plusHours(1)));

            assertThatThrownBy(() -> crearServicio().execute(comando("123456")))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            // Los segundos separan el doble clic del aprobador de la reproduccion de
            // un correo filtrado. Por eso este evento es WARN y los otros INFO.
            verify(audit).approvalDeniedByReplay(4271L, 1800L);
            verify(metrics).resolved(ApprovalResult.TOKEN_CONSUMED);
            verify(secretHasher, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("una solicitud caducada no llega a comprobar el codigo")
        void una_solicitud_caducada_no_comprueba_el_codigo() {
            dadoQueElTokenResuelve(solicitud(0, null, null, AHORA.minusMinutes(1)));

            assertThatThrownBy(() -> crearServicio().execute(comando("123456")))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verify(audit).approvalDenied("token_expired", 4271L);
            verify(metrics).resolved(ApprovalResult.TOKEN_EXPIRED);
            verify(secretHasher, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("codigo de verificacion")
    class CodigoDeVerificacion {

        @Test
        @DisplayName("un codigo incorrecto gasta un intento y sale como 422 con el margen restante")
        void un_codigo_incorrecto_gasta_un_intento() {
            dadoQueElTokenResuelve(solicitud(1, null, null, AHORA.plusHours(1)));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(false);
            when(requestRepository.registerFailedAttempt(4271L)).thenReturn(1);

            assertThatThrownBy(() -> crearServicio().execute(comando("000000")))
                    .isInstanceOf(PlatformAccessCodeMismatchException.class)
                    .extracting(
                            e -> ((PlatformAccessCodeMismatchException) e).getRemainingAttempts())
                    .isEqualTo(3);

            verify(audit).approvalDeniedByCodeMismatch(4271L, 3);
            verify(metrics).resolved(ApprovalResult.CODE_MISMATCH);
            verifyNoInteractions(invitationRepository, emailSender);
        }

        @Test
        @DisplayName("el ultimo intento no sale como 422 con cero: sale como 429 y bloquea")
        void el_ultimo_intento_bloquea() {
            dadoQueElTokenResuelve(solicitud(4, null, null, AHORA.plusHours(1)));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(false);
            when(requestRepository.registerFailedAttempt(4271L)).thenReturn(1);

            // El front trata remainingAttempts == 0 como bloqueo, igual que un 429:
            // las dos rutas convergen, y elegir el 429 evita mandar dos senales para
            // el mismo estado.
            assertThatThrownBy(() -> crearServicio().execute(comando("000000")))
                    .isInstanceOf(PlatformAccessBlockedException.class);

            verify(audit).approvalLocked(4271L);
            verify(metrics).resolved(ApprovalResult.ATTEMPTS_EXHAUSTED);
        }

        @Test
        @DisplayName("una solicitud ya bloqueada sale 429 sin gastar otro intento")
        void una_solicitud_ya_bloqueada_no_gasta_otro_intento() {
            dadoQueElTokenResuelve(solicitud(5, null, null, AHORA.plusHours(1)));

            assertThatThrownBy(() -> crearServicio().execute(comando("123456")))
                    .isInstanceOf(PlatformAccessBlockedException.class);

            verify(requestRepository, never()).registerFailedAttempt(anyLong());
            verify(secretHasher, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("el bloqueo gana a la caducidad: sigue siendo 429 despues de expirar")
        void el_bloqueo_gana_a_la_caducidad() {
            dadoQueElTokenResuelve(solicitud(5, null, null, AHORA.minusMinutes(1)));

            // Si degradara a 404 al caducar, el front volveria a ofrecer el
            // formulario del codigo: evalua el 429 antes que el 422.
            assertThatThrownBy(() -> crearServicio().execute(comando("123456")))
                    .isInstanceOf(PlatformAccessBlockedException.class);
        }

        @Test
        @DisplayName("si el UPDATE del contador no afecta filas, otra peticion agoto los intentos")
        void si_el_update_no_afecta_filas_ya_estaba_bloqueada() {
            dadoQueElTokenResuelve(solicitud(3, null, null, AHORA.plusHours(1)));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(false);
            when(requestRepository.registerFailedAttempt(4271L)).thenReturn(0);

            assertThatThrownBy(() -> crearServicio().execute(comando("000000")))
                    .isInstanceOf(PlatformAccessBlockedException.class);

            verify(audit).approvalLocked(4271L);
        }
    }

    @Nested
    @DisplayName("carrera contra otra pestana")
    class Carrera {

        @Test
        @DisplayName("si la decision no afecta ninguna fila, no se emite invitacion ni correo")
        void si_la_decision_no_afecta_filas_no_se_emite_nada() {
            dadoQueElTokenResuelve(pendiente());
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(0);

            // El UPDATE condicional es lo que decide, no el if de arriba: entre la
            // lectura y esa linea otra pestana pudo aprobar o rechazar.
            assertThatThrownBy(() -> crearServicio().execute(comando("123456")))
                    .isInstanceOf(InvalidApprovalTokenException.class);

            verifyNoInteractions(invitationRepository, emailSender);
            verify(metrics).resolved(ApprovalResult.TOKEN_CONSUMED);
        }
    }
}
