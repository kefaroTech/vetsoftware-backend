package com.vetsoftware.app.legaldocumentversion.application.command;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import java.time.LocalDate;

/**
 * Publica un texto legal nuevo.
 *
 * <p>
 * No trae ni {@code documentVersion} ni {@code contentHash}: el numero lo
 * asigna el servicio a partir del ultimo publicado y la huella la deriva el
 * dominio del propio contenido. Dejar que el cliente eligiera cualquiera de los
 * dos abriria la puerta a una version que se salta el orden o a una huella que
 * no corresponde a su texto.
 */
public record PublishLegalDocumentVersionCommand(String code, LegalDocumentKind kind, String title,
        String content, LocalDate effectiveFrom, Long publishedBySystemUserId) {
}
