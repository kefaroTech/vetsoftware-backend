package com.vetsoftware.app.legaldocumentversion.application.dto;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentVersion;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El texto legal completo. Lleva {@code content} porque el caso de uso que mas
 * importa es releer lo que se acepto, y una prueba que no se puede exhibir no
 * sirve de nada.
 */
public record LegalDocumentVersionDto(Long id, String code, int documentVersion,
        LegalDocumentKind kind, String title, String content, String contentHash,
        LocalDateTime publishedAt, Long publishedBySystemUserId, LocalDate effectiveFrom,
        LocalDateTime supersededAt, boolean current, LocalDateTime createdDate) {

    public static LegalDocumentVersionDto from(LegalDocumentVersion version) {
        return new LegalDocumentVersionDto(version.getId(), version.getCode(),
                version.getDocumentVersion(), version.getKind(), version.getTitle(),
                version.getContent(), version.getContentHash(), version.getPublishedAt(),
                version.getPublishedBySystemUserId(), version.getEffectiveFrom(),
                version.getSupersededAt(), version.isCurrent(), version.getCreatedDate());
    }
}
