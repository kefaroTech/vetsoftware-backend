package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import org.springframework.stereotype.Component;

/** Unico sitio que conoce a la vez el agregado y la fila. */
@Component
public class PlatformAccessInvitationJpaMapper {

    public PlatformAccessInvitationJpaEntity toJpa(PlatformAccessInvitation invitation) {
        PlatformAccessInvitationJpaEntity entity = new PlatformAccessInvitationJpaEntity();
        entity.setId(invitation.getId());
        entity.setAccessRequestId(invitation.getAccessRequestId());
        entity.setTokenHash(invitation.getTokenHash());
        entity.setExpiresAt(invitation.getExpiresAt());
        entity.setConsumedAt(invitation.getConsumedAt());
        entity.setSystemUserId(invitation.getSystemUserId());
        entity.setCreatedDate(invitation.getCreatedDate());
        return entity;
    }

    public PlatformAccessInvitation toDomain(PlatformAccessInvitationJpaEntity entity) {
        return new PlatformAccessInvitation(entity.getId(), entity.getAccessRequestId(),
                entity.getTokenHash(), entity.getExpiresAt(), entity.getConsumedAt(),
                entity.getSystemUserId(), entity.getCreatedDate());
    }
}
