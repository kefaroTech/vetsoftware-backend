package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import org.springframework.stereotype.Component;

@Component
public class LegalDocumentVersionJpaMapper {

    public LegalDocumentVersionJpaEntity toJpa(LegalDocumentVersion version) {
        LegalDocumentVersionJpaEntity entity = new LegalDocumentVersionJpaEntity();
        entity.setId(version.getId());
        entity.setCode(version.getCode());
        entity.setDocumentVersion(version.getDocumentVersion());
        entity.setKind(version.getKind());
        entity.setTitle(version.getTitle());
        entity.setContent(version.getContent());
        entity.setContentHash(version.getContentHash());
        entity.setPublishedAt(version.getPublishedAt());
        entity.setPublishedBySystemUserId(version.getPublishedBySystemUserId());
        entity.setEffectiveFrom(version.getEffectiveFrom());
        entity.setSupersededAt(version.getSupersededAt());
        entity.setCreatedDate(version.getCreatedDate());
        entity.setVersion(version.getVersion());
        return entity;
    }

    public LegalDocumentVersion toDomain(LegalDocumentVersionJpaEntity entity) {
        return new LegalDocumentVersion(entity.getId(), entity.getCode(),
                entity.getDocumentVersion(), entity.getKind(), entity.getTitle(),
                entity.getContent(), entity.getContentHash(), entity.getPublishedAt(),
                entity.getPublishedBySystemUserId(), entity.getEffectiveFrom(),
                entity.getSupersededAt(), entity.getCreatedDate(), entity.getVersion());
    }
}
