package com.vetsoftware.app.legaldocumentversion.infrastructure.web.response;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code contentHash} viaja al front porque es lo que la aceptacion tiene que
 * guardar: sin el, el cliente no podria despues volver a pedir el texto que
 * acepto.
 */
public record LegalDocumentVersionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "TERMS_OF_SERVICE") String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "3") int documentVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LegalDocumentKind kind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "SHA-256 del contenido: la huella con la que se prueba que texto se acepto") String contentHash,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime publishedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long publishedBySystemUserId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate effectiveFrom,
        LocalDateTime supersededAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean current,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static LegalDocumentVersionResponse from(LegalDocumentVersionDto dto) {
        return new LegalDocumentVersionResponse(dto.id(), dto.code(), dto.documentVersion(),
                dto.kind(), dto.title(), dto.content(), dto.contentHash(), dto.publishedAt(),
                dto.publishedBySystemUserId(), dto.effectiveFrom(), dto.supersededAt(),
                dto.current(), dto.createdDate());
    }
}
