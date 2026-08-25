package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformaccess.application.command.AcceptPlatformInvitationCommand;
import com.vetsoftware.app.platformaccess.application.command.RequestPlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessSwitchPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformSystemUserProvisioningPort;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.testsupport.PlatformAccessMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <b>Los cuatro correos del flujo salen DESPUÉS del commit, y ni un instante
 * antes.</b>
 *
 * <p>
 * Es la regla BE-18 del repositorio, y su defecto original fue exactamente
 * éste: una cita que revertía en el flush dejaba al cliente con la confirmación
 * de una cita que no existía. Aquí el daño sería peor — el correo que se
 * adelanta es <b>el enlace que aprueba la creación de un
 * superadministrador</b>, o la invitación que la consuma. Una vez entregado no
 * hay rollback que lo retire.
 *
 * <p>
 * <b>Por qué hace falta este archivo y no bastan los tests de cada
 * servicio.</b> Aquéllos corren sin transacción activa, así que toman la rama
 * de guarda que envía en el acto — legítima y también probada — y <b>nunca
 * ejecutan el callback diferido</b>. Los cuatro {@code afterCommit} estaban al
 * 0 % de cobertura: el código que decide cuándo sale el correo no lo ejercitaba
 * nadie.
 *
 * <p>
 * Cada caso comprueba las tres cosas que importan: que al terminar
 * {@code execute} <b>todavía no se ha enviado nada</b>, que el envío ocurre al
 * confirmar, y que una excepción del remitente <b>no se propaga</b> —si lo
 * hiciera, convertiría en un 500 una operación que ya está confirmada en la
 * base—.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Los cuatro correos del alta se difieren al commit (BE-18)")
class PlatformAccessAfterCommitTest {

    @Mock
    private PlatformAccessRequestRepository requestRepository;
    @Mock
    private PlatformAccessInvitationRepository invitationRepository;
    @Mock
    private PlatformAccessSwitchPort accessSwitch;
    @Mock
    private PlatformSystemUserProvisioningPort provisioning;
    @Mock
    private SecretHasherPort secretHasher;
    @Mock
    private PlatformAccessEmailSender emailSender;
    @Mock
    private PlatformAccessAuditPort audit;
    @Mock
    private PlatformAccessMetrics metrics;

    @BeforeEach
    void abrirLaSincronizacionDeTransaccion() {
        // Es lo que hace el TransactionInterceptor al entrar en un @Transactional.
        // Sin esto, los servicios toman la rama de envio inmediato.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cerrarLaSincronizacionDeTransaccion() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    /** Lo que hace el gestor de transacciones tras un commit correcto. */
    private void confirmarLaTransaccion() {
        List.copyOf(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    private void elEfectoQuedoDiferido() {
        assertThat(TransactionSynchronizationManager.getSynchronizations())
                .as("el efecto no quedo registrado para despues del commit").hasSize(1);
    }

    @Nested
    @DisplayName("aviso al aprobador")
    class AvisoAlAprobador {

        private void ejecutar() {
            when(accessSwitch.isOpen()).thenReturn(true);
            when(requestRepository.findLivePendingByEmail(anyString(), any()))
                    .thenReturn(Optional.empty());
            when(secretHasher.hash(anyString())).thenReturn(PlatformAccessMother.HASH_CODIGO);
            when(requestRepository.save(any()))
                    .thenReturn(PlatformAccessMother.solicitudPendiente());

            new RequestPlatformAccessService(accessSwitch, requestRepository, secretHasher,
                    emailSender, audit, metrics, PlatformAccessMother.RELOJ, 72L, 5)
                    .execute(new RequestPlatformAccessCommand(PlatformAccessMother.NOMBRE,
                            PlatformAccessMother.CORREO, PlatformAccessMother.MOTIVO));
        }

        @Test
        @DisplayName("el enlace de aprobacion NO sale mientras la transaccion sigue abierta")
        void el_enlace_no_sale_antes_del_commit() {
            ejecutar();

            // Si saliera aqui, un rollback posterior dejaria en el buzon del aprobador
            // el enlace de una solicitud que no existe, y ese enlace acuna
            // superadministradores.
            verify(emailSender, never()).sendAccessRequested(any());
            elEfectoQuedoDiferido();
        }

        @Test
        @DisplayName("sale al confirmar, con el token y el codigo planos capturados en memoria")
        void sale_al_confirmar() {
            ejecutar();

            confirmarLaTransaccion();

            verify(emailSender).sendAccessRequested(any());
        }

        @Test
        @DisplayName("si el envio revienta, la excepcion NO se propaga: la solicitud ya esta confirmada")
        void si_el_envio_revienta_no_se_propaga() {
            ejecutar();
            doThrow(new IllegalStateException("Resend caido")).when(emailSender)
                    .sendAccessRequested(any());

            // Una excepcion en afterCommit llega al llamador con la transaccion ya
            // confirmada, y convertiria un 202 correcto en un 500.
            assertThatCode(PlatformAccessAfterCommitTest.this::confirmarLaTransaccion)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invitacion tras aprobar")
    class InvitacionTrasAprobar {

        private void ejecutar() {
            when(requestRepository.findByApprovalTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);
            when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            new ApprovePlatformAccessRequestService(requestRepository, invitationRepository,
                    secretHasher, emailSender, audit, metrics, PlatformAccessMother.RELOJ, 7L)
                    .execute(new ResolvePlatformAccessCommand(PlatformAccessMother.TOKEN_PLANO,
                            PlatformAccessMother.CODIGO));
        }

        @Test
        @DisplayName("la invitacion NO se entrega antes del commit")
        void la_invitacion_no_se_entrega_antes_del_commit() {
            ejecutar();

            verify(emailSender, never()).sendInvitation(anyLong(), anyString(), anyString(),
                    anyString());
            elEfectoQuedoDiferido();
        }

        @Test
        @DisplayName("se entrega al confirmar")
        void se_entrega_al_confirmar() {
            ejecutar();

            confirmarLaTransaccion();

            verify(emailSender).sendInvitation(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("un fallo del envio no propaga: el correo se pierde y lo registra el adaptador")
        void un_fallo_del_envio_no_propaga() {
            ejecutar();
            doThrow(new IllegalStateException("Resend caido")).when(emailSender)
                    .sendInvitation(anyLong(), anyString(), anyString(), anyString());

            assertThatCode(PlatformAccessAfterCommitTest.this::confirmarLaTransaccion)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("aviso de rechazo")
    class AvisoDeRechazo {

        private void ejecutar() {
            when(requestRepository.findByApprovalTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));
            when(secretHasher.matches(anyString(), anyString())).thenReturn(true);
            when(requestRepository.applyDecision(anyLong(), any(), any())).thenReturn(1);

            new RejectPlatformAccessRequestService(requestRepository, secretHasher, emailSender,
                    audit, metrics, PlatformAccessMother.RELOJ)
                    .execute(new ResolvePlatformAccessCommand(PlatformAccessMother.TOKEN_PLANO,
                            PlatformAccessMother.CODIGO));
        }

        @Test
        @DisplayName("el aviso NO sale antes del commit")
        void el_aviso_no_sale_antes_del_commit() {
            ejecutar();

            verify(emailSender, never()).sendRejection(anyLong(), anyString(), anyString());
            elEfectoQuedoDiferido();
        }

        @Test
        @DisplayName("sale al confirmar")
        void sale_al_confirmar() {
            ejecutar();

            confirmarLaTransaccion();

            verify(emailSender).sendRejection(PlatformAccessMother.ID_SOLICITUD,
                    PlatformAccessMother.CORREO, PlatformAccessMother.NOMBRE);
        }

        @Test
        @DisplayName("un fallo del envio no propaga")
        void un_fallo_del_envio_no_propaga() {
            ejecutar();
            doThrow(new IllegalStateException("Resend caido")).when(emailSender)
                    .sendRejection(anyLong(), anyString(), anyString());

            assertThatCode(PlatformAccessAfterCommitTest.this::confirmarLaTransaccion)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("bienvenida con el codigo de usuario")
    class Bienvenida {

        private void ejecutar() {
            when(invitationRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(PlatformAccessMother.invitacionViva()));
            when(requestRepository.findById(anyLong()))
                    .thenReturn(Optional.of(PlatformAccessMother.solicitudPendiente()));
            when(provisioning.emailTaken(anyString())).thenReturn(false);
            when(provisioning.codeTaken(anyString())).thenReturn(false);
            when(secretHasher.hash(anyString())).thenReturn("$2a$12$hash");
            when(provisioning.provision(anyString(), anyString(), anyString(), anyString(), any()))
                    .thenReturn(9001L);
            when(invitationRepository.consume(anyLong(), anyLong(), any())).thenReturn(1);

            new AcceptPlatformInvitationService(invitationRepository, requestRepository,
                    provisioning, secretHasher, emailSender, audit, metrics,
                    PlatformAccessMother.RELOJ).execute(
                            new AcceptPlatformInvitationCommand("token-inv", "contrasena-larga-1"));
        }

        @Test
        @DisplayName("la bienvenida NO sale antes del commit")
        void la_bienvenida_no_sale_antes_del_commit() {
            ejecutar();

            verify(emailSender, never()).sendWelcome(anyLong(), anyString(), anyString(),
                    anyString());
            elEfectoQuedoDiferido();
        }

        @Test
        @DisplayName("sale al confirmar con el codigo de login, sin el cual la cuenta no se puede usar")
        void sale_al_confirmar_con_el_codigo_de_login() {
            ejecutar();

            confirmarLaTransaccion();

            verify(emailSender).sendWelcome(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("un fallo del envio no propaga, aunque deje la cuenta sin forma de entrar")
        void un_fallo_del_envio_no_propaga() {
            ejecutar();
            doThrow(new IllegalStateException("Resend caido")).when(emailSender)
                    .sendWelcome(anyLong(), anyString(), anyString(), anyString());

            // El alta ya ocurrio y es irreversible: propagar aqui daria un 500 sobre
            // una cuenta que SI existe. La perdida del codigo esta registrada aparte.
            assertThatCode(PlatformAccessAfterCommitTest.this::confirmarLaTransaccion)
                    .doesNotThrowAnyException();
        }
    }
}
