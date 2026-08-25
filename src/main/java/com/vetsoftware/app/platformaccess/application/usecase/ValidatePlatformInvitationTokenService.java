package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.dto.PlatformInvitationDto;
import com.vetsoftware.app.platformaccess.application.port.in.ValidatePlatformInvitationTokenUseCase;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessAuditPort;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessRequestRepository;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el enlace de la invitación para pintar la pantalla de crear
 * contraseña. Devuelve el correo, que es lo único que esa pantalla necesita, y
 * lo saca de la solicitud a la que apunta la invitación: la cadena
 * {@code token_hash -> access_request_id -> email} va toda por clave única y no
 * tiene ningún eslabón que el cliente pueda torcer.
 *
 * <p>
 * No consume la invitación: montar la pantalla no puede gastar el único uso que
 * tiene.
 */
@Observed(name = "system.user.invitation.validate.token")
@Service
public class ValidatePlatformInvitationTokenService
        implements
            ValidatePlatformInvitationTokenUseCase {

    private final PlatformAccessInvitationRepository invitationRepository;
    private final PlatformAccessRequestRepository requestRepository;
    private final PlatformAccessAuditPort audit;
    private final Clock clock;

    public ValidatePlatformInvitationTokenService(
            PlatformAccessInvitationRepository invitationRepository,
            PlatformAccessRequestRepository requestRepository, PlatformAccessAuditPort audit,
            Clock clock) {
        this.invitationRepository = invitationRepository;
        this.requestRepository = requestRepository;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformInvitationDto execute(String rawToken) {
        PlatformAccessInvitation invitation = PlatformInvitations.locate(invitationRepository,
                rawToken);
        LocalDateTime now = LocalDateTime.now(clock);

        audit.bindRequest(invitation.getAccessRequestId());
        try {
            if (!invitation.isUsable(now)) {
                throw new InvalidInvitationTokenException("Invitation is no longer usable");
            }
            PlatformAccessRequest request = requestRepository
                    .findById(invitation.getAccessRequestId())
                    .orElseThrow(() -> new InvalidInvitationTokenException(
                            "Invitation has no readable access request"));
            return new PlatformInvitationDto(request.getEmail());
        } finally {
            audit.unbindRequest();
        }
    }
}
