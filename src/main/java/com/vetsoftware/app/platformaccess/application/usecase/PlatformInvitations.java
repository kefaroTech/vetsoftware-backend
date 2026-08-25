package com.vetsoftware.app.platformaccess.application.usecase;

import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessInvitationRepository;
import com.vetsoftware.app.platformaccess.domain.InvalidInvitationTokenException;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;

/**
 * Localización del token de invitación, compartida por validar y aceptar.
 *
 * <p>
 * El token vacío y el token inexistente producen la misma excepción con el
 * mismo mensaje: distinguirlos diría qué invitaciones existieron alguna vez.
 */
final class PlatformInvitations {

    private PlatformInvitations() {
    }

    static PlatformAccessInvitation locate(PlatformAccessInvitationRepository repository,
            String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidInvitationTokenException("Invitation token is required");
        }
        return repository.findByTokenHash(PlatformAccessTokens.hash(rawToken)).orElseThrow(
                () -> new InvalidInvitationTokenException("Invitation token does not exist"));
    }
}
