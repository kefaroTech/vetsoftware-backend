package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.command.ResolvePlatformAccessCommand;
import com.vetsoftware.app.platformaccess.application.port.in.RejectPlatformAccessRequestUseCase;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessEmailSender;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.ApprovalResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Rechaza la solicitud. Exige el mismo token y el mismo código que aprobar —no
 * hay un código por decisión— y aplica exactamente las mismas comprobaciones:
 * quien puede rechazar puede aprobar, así que relajar el lado del rechazo sería
 * dejar que alguien queme solicitudes ajenas con menos credencial.
 *
 * <p>
 * El rechazo es terminal: no emite invitación y el solicitante recibe un aviso
 * <b>sin motivo</b>. Explicarlo convertiría el correo en un canal para deducir
 * qué criterios usa quien aprueba.
 */
@Observed(name = "system.user.request.reject")
@Service
public class RejectPlatformAccessRequestService implements RejectPlatformAccessRequestUseCase {

    private static final Logger log = LoggerFactory
            .getLogger(RejectPlatformAccessRequestService.class);

    private final PlatformAccessRequestRepository requestRepository;
    private final SecretHasherPort secretHasher;
    private final PlatformAccessEmailSender emailSender;
    private final PlatformAccessAuditPort audit;
    private final PlatformAccessMetrics metrics;
    private final Clock clock;

    public RejectPlatformAccessRequestService(PlatformAccessRequestRepository requestRepository,
            SecretHasherPort secretHasher, PlatformAccessEmailSender emailSender,
            PlatformAccessAuditPort audit, PlatformAccessMetrics metrics, Clock clock) {
        this.requestRepository = requestRepository;
        this.secretHasher = secretHasher;
        this.emailSender = emailSender;
        this.audit = audit;
        this.metrics = metrics;
        this.clock = clock;
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

            if (requestRepository.applyDecision(request.getId(), PlatformAccessDecision.REJECTED,
                    now) == 0) {
                audit.approvalDeniedByReplay(request.getId(), 0L);
                metrics.resolved(ApprovalResult.TOKEN_CONSUMED);
                throw new InvalidApprovalTokenException("Access request is no longer resolvable");
            }

            audit.requestRejected(request.getId());
            metrics.resolved(ApprovalResult.REJECTED);
            sendAfterCommit(request.getId(), request.getEmail(), request.getFullName());
        } finally {
            audit.unbindRequest();
        }
    }

    /**
     * Difiere el aviso de rechazo al commit. Clase anónima y no lambda: la regla
     * que vigila este patrón atribuye el lambda al método declarante y da un falso
     * positivo.
     */
    private void sendAfterCommit(Long requestId, String toEmail, String fullName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailSender.sendRejection(requestId, toEmail, fullName);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailSender.sendRejection(requestId, toEmail, fullName);
                } catch (RuntimeException exception) {
                    log.warn("No se pudo encolar el aviso de rechazo de acceso de plataforma",
                            exception);
                }
            }
        });
    }
}
