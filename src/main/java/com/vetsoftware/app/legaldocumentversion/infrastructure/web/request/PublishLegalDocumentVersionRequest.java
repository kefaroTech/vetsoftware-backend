package com.vetsoftware.app.legaldocumentversion.infrastructure.web.request;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * <strong>Sin {@code documentVersion} y sin {@code contentHash}</strong>: el
 * numero lo asigna el servidor a partir del ultimo publicado y la huella la
 * deriva el dominio del propio contenido. Un cliente que pudiera elegirlos
 * podria saltarse el orden de versiones o declarar una huella que no
 * corresponde a su texto —y esa huella es justo lo que despues prueba que
 * alguien acepto ese texto y no otro—.
 *
 * <p>
 * Tampoco lleva {@code publishedBySystemUserId}: lo pone el controller desde el
 * principal, igual que la empresa en los recursos del tenant.
 */
public record PublishLegalDocumentVersionRequest(@NotBlank @Size(max = 50) String code,
        @NotNull LegalDocumentKind kind, @NotBlank @Size(max = 200) String title,
        @NotBlank String content, @NotNull LocalDate effectiveFrom) {
}
