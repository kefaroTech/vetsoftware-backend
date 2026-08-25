package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.dto.PlatformAccessRequestDto;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformAccessTokenUseCase;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.domain.InvalidApprovalTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el enlace del aprobador para pintar la solicitud. No consume, no
 * decide y <b>no gasta intentos</b>: abrir el enlace dos veces no puede acercar
 * a nadie al bloqueo.
 *
 * <p>
 * Los cuatro estados muertos salen como la misma excepción, y eso incluye el
 * bloqueado: aquí no se emite 429. La pantalla de aprobación solo distingue
 * "enlace vivo" de "enlace muerto", y devolver un 429 en esta lectura diría que
 * el token existió y que alguien estuvo probando códigos contra él.
 */
@Observed(name = "system.user.request.validate.token")
@Service
public class ValidatePlatformAccessTokenService implements ValidatePlatformAccessTokenUseCase {

    private final PlatformAccessRequestRepository requestRepository;
    private final PlatformAccessAuditPort audit;
    private final PlatformAccessMetrics metrics;
    private final Clock clock;

    public ValidatePlatformAccessTokenService(PlatformAccessRequestRepository requestRepository,
            PlatformAccessAuditPort audit, PlatformAccessMetrics metrics, Clock clock) {
        this.requestRepository = requestRepository;
        this.audit = audit;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformAccessRequestDto execute(String rawToken) {
        PlatformAccessRequest request = PlatformAccessDecisions.locate(requestRepository, audit,
                metrics, rawToken);
        LocalDateTime now = LocalDateTime.now(clock);

        audit.bindRequest(request.getId());
        try {
            if (!request.isPending(now)) {
                // Un solo evento con el motivo real; hacia fuera, un solo código.
                audit.approvalDenied(reasonFor(request, now), request.getId());
                metrics.resolved(resultFor(request, now));
                throw new InvalidApprovalTokenException("Access request is no longer resolvable");
            }
            return PlatformAccessRequestDto.from(request);
        } finally {
            audit.unbindRequest();
        }
    }

    private static String reasonFor(PlatformAccessRequest request, LocalDateTime now) {
        if (request.isBlocked()) {
            return "attempts_exhausted";
        }
        if (request.isDecided()) {
            return "token_consumed";
        }
        return request.isExpired(now) ? "token_expired" : "token_invalid";
    }

    private static PlatformAccessMetrics.ApprovalResult resultFor(PlatformAccessRequest request,
            LocalDateTime now) {
        if (request.isBlocked()) {
            return PlatformAccessMetrics.ApprovalResult.ATTEMPTS_EXHAUSTED;
        }
        if (request.isDecided()) {
            return PlatformAccessMetrics.ApprovalResult.TOKEN_CONSUMED;
        }
        return request.isExpired(now)
                ? PlatformAccessMetrics.ApprovalResult.TOKEN_EXPIRED
                : PlatformAccessMetrics.ApprovalResult.TOKEN_INVALID;
    }
}
