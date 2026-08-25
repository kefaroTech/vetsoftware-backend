package com.vetsoftware.app.platformaccess.infrastructure.audit;

import com.vetsoftware.app.infrastructure.audit.AuditLogger;
import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Emite los eventos del flujo por el canal AUDIT unico del sistema y administra
 * el identificador de correlacion.
 *
 * <p>
 * <b>Por que el id va tambien como atributo de span, y por que en la rama de
 * alta cardinalidad.</b> En Micrometer, lo <i>low</i> viaja a metricas Y a
 * trazas; lo <i>high</i> solo a trazas. Un id de solicitud como etiqueta de
 * metrica seria una serie por solicitud, que es el fallo mas caro del catalogo.
 * Como atributo de traza, en cambio, es lo que permite recuperar en Tempo las
 * tres peticiones del flujo, separadas por horas o dias, que por eso mismo no
 * comparten {@code traceId}: el contexto de traza W3C identifica una operacion
 * distribuida, no un proceso de negocio con un humano dentro.
 *
 * <p>
 * {@code traceId} y {@code spanId} no se tocan jamas: son propiedad exclusiva
 * de Micrometer Tracing.
 */
@Component
public class PlatformAccessAuditAdapter implements PlatformAccessAuditPort {

    private final AuditLogger auditLogger;
    private final ObservationRegistry observationRegistry;

    public PlatformAccessAuditAdapter(AuditLogger auditLogger,
            ObservationRegistry observationRegistry) {
        this.auditLogger = auditLogger;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public void bindRequest(Long requestId) {
        if (requestId == null) {
            return;
        }
        MDC.put(MdcKeys.SYSTEM_USER_REQUEST_ID, String.valueOf(requestId));
        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            current.highCardinalityKeyValue(MdcKeys.SYSTEM_USER_REQUEST_ID,
                    String.valueOf(requestId));
        }
    }

    @Override
    public void unbindRequest() {
        MDC.remove(MdcKeys.SYSTEM_USER_REQUEST_ID);
    }

    @Override
    public void accessRequested(Long requestId, String emailDomain) {
        auditLogger.systemUserRequested(requestId, emailDomain);
    }

    @Override
    public void accessRequestDenied(String reason, Long requestId, String emailDomain) {
        auditLogger.systemUserRequestDenied(reason, requestId, emailDomain);
    }

    @Override
    public void approvalDenied(String reason, Long requestId) {
        auditLogger.systemUserApprovalDenied(reason, requestId);
    }

    @Override
    public void approvalDeniedByReplay(Long requestId, long secondsSinceConsumption) {
        auditLogger.systemUserApprovalReplayed(requestId, secondsSinceConsumption);
    }

    @Override
    public void approvalDeniedByCodeMismatch(Long requestId, int remainingAttempts) {
        auditLogger.systemUserApprovalCodeMismatch(requestId, remainingAttempts);
    }

    @Override
    public void approvalLocked(Long requestId) {
        auditLogger.systemUserApprovalLocked(requestId);
    }

    @Override
    public void requestApproved(Long requestId) {
        auditLogger.systemUserRequestApproved(requestId);
    }

    @Override
    public void requestRejected(Long requestId) {
        auditLogger.systemUserRequestRejected(requestId);
    }

    @Override
    public void invited(Long requestId, String emailDomain) {
        auditLogger.systemUserInvited(requestId, emailDomain);
    }

    @Override
    public void invitationDenied(String reason, Long requestId) {
        auditLogger.systemUserInvitationDenied(reason, requestId);
    }

    @Override
    public void invitationUndelivered(Long requestId, String emailDomain) {
        auditLogger.systemUserInvitationUndelivered(requestId, emailDomain);
    }

    @Override
    public void welcomeUndelivered(Long requestId, String emailDomain) {
        auditLogger.systemUserWelcomeUndelivered(requestId, emailDomain);
    }

    @Override
    public void systemUserProvisioned(Long requestId, Long systemUserId) {
        auditLogger.systemUserProvisioned(requestId, systemUserId);
    }
}
