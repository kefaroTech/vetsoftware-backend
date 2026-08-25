package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.in.ApprovePlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.ApprovalResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Aprueba la solicitud y emite la invitación.
 *
 * <p>
 * <b>El código de 6 dígitos NO es un segundo factor.</b> Viaja en el mismo
 * correo que el enlace, por decisión humana explícita tomada con el riesgo
 * delante: quien tenga acceso al buzón del aprobador tiene los dos elementos.
 * Su función es <b>confirmar la intención</b> —evitar la aprobación por un clic
 * accidental sobre el enlace— y separar «alguien reenvió el correo entero» de
 * «el aprobador decidió». El contador de 5 intentos sigue siendo necesario:
 * protege el caso de quien obtuvo o adivinó el token pero no el correo. Un
 * factor fuera de banda real (secreto pre-compartido o TOTP) es una mejora
 * posterior y no está aquí.
 *
 * <p>
 * <b>La autorización es la posesión del token más el código, y nada más.</b> No
 * hay aprobador modelado: ni tabla, ni rol, ni FK, ni columna. El destinatario
 * del enlace es un correo fijo de configuración, así que el correo que acuña
 * superadministradores vive en el parameter store y no en una tabla —un
 * {@code UPDATE} en la base no basta para redirigirlo—.
 *
 * <p>
 * <b>Escalada de privilegio.</b> El filtro de autenticación concede
 * {@code ROLE_SYSTEM} a toda cuenta de sistema sin mirar permisos, así que lo
 * que este flujo termina creando tiene control total. Por eso aquí no se
 * aceptan permisos, roles ni banderas del cliente, no se llama a ningún caso de
 * uso de permisos y no se copian los de ninguna otra cuenta.
 */
@Observed(name = "system.user.request.approve")
@Service
public class ApprovePlatformAccessRequestService implements ApprovePlatformAccessRequestUseCase {

    private static final Logger log = LoggerFactory
            .getLogger(ApprovePlatformAccessRequestService.class);

    private final PlatformAccessRequestRepository requestRepository;
    private final PlatformAccessInvitationRepository invitationRepository;
    private final SecretHasherPort secretHasher;
    private final PlatformAccessEmailSender emailSender;
    private final PlatformAccessAuditPort audit;
    private final PlatformAccessMetrics metrics;
    private final Clock clock;
    private final long invitationTtlDays;

    public ApprovePlatformAccessRequestService(PlatformAccessRequestRepository requestRepository,
            PlatformAccessInvitationRepository invitationRepository, SecretHasherPort secretHasher,
            PlatformAccessEmailSender emailSender, PlatformAccessAuditPort audit,
            PlatformAccessMetrics metrics, Clock clock,
            @Value("${vetsoftware.platform-access.invitation-ttl-days:7}") long invitationTtlDays) {
        this.requestRepository = requestRepository;
        this.invitationRepository = invitationRepository;
        this.secretHasher = secretHasher;
        this.emailSender = emailSender;
        this.audit = audit;
        this.metrics = metrics;
        this.clock = clock;
        this.invitationTtlDays = invitationTtlDays;
    }

    @Override
    @Transactional
    public void execute(ResolvePlatformAccessCommand command) {
        PlatformAccessRequest request = PlatformAccessDecisions.locate(requestRepository, audit,
                metrics, command.token());
        LocalDateTime now = LocalDateTime.now(clock);

        audit.bindRequest(request.getId());
        try {
            PlatformAccessDecisions.verify(requestRepository, secretHasher, audit, metrics, request,
                    command.code(), now);

            // El UPDATE condicional es lo que decide, no el if de arriba: entre la
            // lectura y esta línea otra pestaña pudo aprobar o rechazar la misma
            // solicitud. rowcount = 0 significa que perdimos la carrera.
            if (requestRepository.applyDecision(request.getId(), PlatformAccessDecision.APPROVED,
                    now) == 0) {
                audit.approvalDeniedByReplay(request.getId(), 0L);
                metrics.resolved(ApprovalResult.TOKEN_CONSUMED);
                throw new InvalidApprovalTokenException("Access request is no longer resolvable");
            }

            String rawInvitationToken = PlatformAccessTokens.generateRawToken();
            invitationRepository.save(PlatformAccessInvitation.issue(request.getId(),
                    PlatformAccessTokens.hash(rawInvitationToken), now,
                    now.plusDays(invitationTtlDays)));

            audit.requestApproved(request.getId());
            metrics.resolved(ApprovalResult.APPROVED);
            sendAfterCommit(request.getId(), request.getEmail(), request.getFullName(),
                    rawInvitationToken);
        } finally {
            audit.unbindRequest();
        }
    }

    /**
     * Difiere la invitación al commit. Clase anónima y no lambda: la regla que
     * vigila este patrón atribuye el lambda al método declarante y da un falso
     * positivo.
     *
     * <p>
     * El token plano viaja capturado en el callback; en la base solo quedó su hash.
     * Si el envío falla, el correo se pierde sin reintento y el adaptador lo
     * registra en ERROR: es el único fallo terminal del flujo, y la salida es que
     * una persona reemita la invitación.
     */
    private void sendAfterCommit(Long requestId, String toEmail, String fullName,
            String rawInvitationToken) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailSender.sendInvitation(requestId, toEmail, fullName, rawInvitationToken);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailSender.sendInvitation(requestId, toEmail, fullName, rawInvitationToken);
                } catch (RuntimeException exception) {
                    log.warn("No se pudo encolar la invitacion de acceso de plataforma", exception);
                }
            }
        });
    }
}
