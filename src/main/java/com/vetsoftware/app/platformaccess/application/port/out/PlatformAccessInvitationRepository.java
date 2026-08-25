package com.vetsoftware.app.platformaccess.application.port.out;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import java.time.LocalDateTime;
import java.util.Optional;

/** Persistencia de la invitacion de un solo uso. */
public interface PlatformAccessInvitationRepository {

    PlatformAccessInvitation save(PlatformAccessInvitation invitation);

    Optional<PlatformAccessInvitation> findByTokenHash(String tokenHash);

    /**
     * Marca la invitacion como consumida y la ata al usuario creado, SOLO si seguia
     * sin consumir.
     *
     * @return filas afectadas; {@code 0} significa que otra peticion la consumio
     *         primero.
     */
    int consume(Long id, Long systemUserId, LocalDateTime now);
}
