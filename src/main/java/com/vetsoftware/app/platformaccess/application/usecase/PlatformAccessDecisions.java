package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics.ApprovalResult;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.application.port.out.SecretHasherPort;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessBlockedException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessCodeMismatchException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import java.time.LocalDateTime;

/**
 * La mitad que aprobar y rechazar tienen en común: localizar la solicitud por
 * el hash del token y verificar el código de 6 dígitos.
 *
 * <p>
 * Es una clase de estáticos y no un bean a propósito. La regla del repositorio
 * es un service por caso de uso —aprobar y rechazar son dos—, y un colaborador
 * con estado en medio solo añadiría un tercer sitio donde mirar sin quitar
 * ninguno de los dos.
 *
 * <p>
 * <b>La precedencia de los tres estados no es cosmética.</b> Bloqueado gana a
 * caducado y caducado a pendiente: un 429 tiene que seguir siendo 429 después
 * de que el enlace caduque, o el front vuelve a ofrecer el formulario porque
 * evalúa el 429 antes que el 422.
 */
final class PlatformAccessDecisions {

    private PlatformAccessDecisions() {
    }

    /**
     * Resuelve el token del aprobador. Los dos caminos de fallo —token vacío y
     * token que no existe— emiten el mismo evento y la misma excepción: aquí
     * todavía no hay solicitud a la que atribuir nada.
     */
    static PlatformAccessRequest locate(PlatformAccessRequestRepository repository,
            PlatformAccessAuditPort audit, PlatformAccessMetrics metrics, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return denyAsInvalid(audit, metrics, "Approval token is required");
        }
        return repository.findByApprovalTokenHash(PlatformAccessTokens.hash(rawToken))
                .orElseGet(() -> denyAsInvalid(audit, metrics, "Approval token does not exist"));
    }

    /**
     * Comprueba que la solicitud sigue siendo decidible y que el código coincide.
     * Devuelve sin más si todo está bien; en cualquier otro caso lanza la excepción
     * que corresponde al estado.
     *
     * <p>
     * La comparación la hace bcrypt dentro del adaptador de
     * {@link SecretHasherPort}: no cortocircuita en el primer byte distinto, así
     * que no filtra por latencia cuántos dígitos se acertaron.
     */
    static void verify(PlatformAccessRequestRepository repository, SecretHasherPort secretHasher,
            PlatformAccessAuditPort audit, PlatformAccessMetrics metrics,
            PlatformAccessRequest request, String rawCode, LocalDateTime now) {
        if (request.isBlocked()) {
            audit.approvalLocked(request.getId());
            metrics.resolved(ApprovalResult.ATTEMPTS_EXHAUSTED);
            throw new PlatformAccessBlockedException("Access request is permanently blocked");
        }
        if (request.isDecided()) {
            // Un token de un solo uso que se vuelve a presentar no es un error de
            // tecleo: o el enlace se filtró, o alguien está reproduciendo el correo.
            // Los segundos transcurridos separan el doble clic de la reproducción.
            audit.approvalDeniedByReplay(request.getId(), request.secondsSinceDecision(now));
            metrics.resolved(ApprovalResult.TOKEN_CONSUMED);
            throw new InvalidApprovalTokenException("Access request was already resolved");
        }
        if (request.isExpired(now)) {
            audit.approvalDenied("token_expired", request.getId());
            metrics.resolved(ApprovalResult.TOKEN_EXPIRED);
            throw new InvalidApprovalTokenException("Approval token expired");
        }
        String code = rawCode == null ? "" : rawCode.trim();
        if (!secretHasher.matches(code, request.getVerificationCodeHash())) {
            registerFailedAttempt(repository, audit, metrics, request);
        }
    }

    /**
     * Gasta un intento y decide si el resultado es un 422 con margen o un 429
     * terminal.
     *
     * <p>
     * <b>El incremento se confirma aunque esta transacción haga rollback.</b> El
     * método del repositorio corre en su propia transacción ({@code REQUIRES_NEW});
     * si no lo hiciera, la excepción que lanzamos justo después desharía el
     * contador y el atacante tendría intentos infinitos. Es la clase de defecto que
     * ningún test de servicio con dobles llega a ver.
     *
     * <p>
     * El margen restante se calcula en memoria y no releyendo la fila: la
     * transacción exterior ya fijó su snapshot bajo REPEATABLE READ, así que una
     * relectura devolvería el contador de antes del incremento. Es una aproximación
     * aceptable —el contrato declara {@code remainingAttempts} opcional y el front
     * lo trata como tal— y nunca sobreestima el margen.
     */
    private static void registerFailedAttempt(PlatformAccessRequestRepository repository,
            PlatformAccessAuditPort audit, PlatformAccessMetrics metrics,
            PlatformAccessRequest request) {
        int updated = repository.registerFailedAttempt(request.getId());
        int remaining = updated == 0
                ? 0
                : Math.max(0, request.getMaxAttempts() - request.getVerificationAttempts() - 1);
        if (remaining <= 0) {
            audit.approvalLocked(request.getId());
            metrics.resolved(ApprovalResult.ATTEMPTS_EXHAUSTED);
            throw new PlatformAccessBlockedException("Access request is permanently blocked");
        }
        audit.approvalDeniedByCodeMismatch(request.getId(), remaining);
        metrics.resolved(ApprovalResult.CODE_MISMATCH);
        throw new PlatformAccessCodeMismatchException("Verification code does not match",
                remaining);
    }

    private static PlatformAccessRequest denyAsInvalid(PlatformAccessAuditPort audit,
            PlatformAccessMetrics metrics, String message) {
        audit.approvalDenied("token_invalid", null);
        metrics.resolved(ApprovalResult.TOKEN_INVALID);
        throw new InvalidApprovalTokenException(message);
    }
}
