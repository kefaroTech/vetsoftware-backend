package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import org.springframework.stereotype.Component;

/** Unico sitio que conoce a la vez el agregado y la fila. */
@Component
public class PlatformAccessRequestJpaMapper {

    public PlatformAccessRequestJpaEntity toJpa(PlatformAccessRequest request) {
        PlatformAccessRequestJpaEntity entity = new PlatformAccessRequestJpaEntity();
        entity.setId(request.getId());
        entity.setFullName(request.getFullName());
        entity.setEmail(request.getEmail());
        entity.setReason(request.getReason());
        entity.setApprovalTokenHash(request.getApprovalTokenHash());
        entity.setVerificationCodeHash(request.getVerificationCodeHash());
        entity.setVerificationAttempts(request.getVerificationAttempts());
        entity.setMaxAttempts((short) request.getMaxAttempts());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setDecision(request.getDecision() == null ? null : request.getDecision().name());
        entity.setDecidedAt(request.getDecidedAt());
        entity.setCreatedDate(request.getCreatedDate());
        entity.setVersion(request.getVersion());
        return entity;
    }

    public PlatformAccessRequest toDomain(PlatformAccessRequestJpaEntity entity) {
        return new PlatformAccessRequest(entity.getId(), entity.getFullName(), entity.getEmail(),
                entity.getReason(), entity.getApprovalTokenHash(), entity.getVerificationCodeHash(),
                entity.getVerificationAttempts(), entity.getMaxAttempts(), entity.getExpiresAt(),
                PlatformAccessDecision.fromNullable(entity.getDecision()), entity.getDecidedAt(),
                entity.getCreatedDate(), entity.getVersion());
    }
}
