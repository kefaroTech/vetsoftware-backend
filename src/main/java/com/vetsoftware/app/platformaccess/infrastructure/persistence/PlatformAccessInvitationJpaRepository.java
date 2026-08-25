package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PlatformAccessInvitationJpaRepository
        extends
            JpaRepository<PlatformAccessInvitationJpaEntity, Long> {

    Optional<PlatformAccessInvitationJpaEntity> findByTokenHash(String tokenHash);

    /**
     * Consume la invitacion, y solo si seguia sin consumir. Es la barrera real
     * contra el doble uso: una comprobacion previa en Java la pierde la
     * concurrencia, y este {@code WHERE} no.
     *
     * <p>
     * El {@code SET} no mueve {@code version} porque esta entidad no lleva bloqueo
     * optimista —esta exenta como token de un solo uso— y la columna existe en la
     * tabla solo para no tener que hacer un {@code ALTER} sobre datos el dia que se
     * decida versionarla. Si alguien le pone {@code @Version}, esta consulta tiene
     * que ganar su {@code version = version + 1} el mismo dia.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE platform_access_invitations
            SET consumed_at = :now, system_user_id = :systemUserId
            WHERE id = :id
              AND consumed_at IS NULL
            """, nativeQuery = true)
    int consume(@Param("id") Long id, @Param("systemUserId") Long systemUserId,
            @Param("now") LocalDateTime now);
}
