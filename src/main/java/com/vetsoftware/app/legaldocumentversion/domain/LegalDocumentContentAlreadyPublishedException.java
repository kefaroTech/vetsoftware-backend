package com.vetsoftware.app.legaldocumentversion.domain;

/**
 * Ese texto exacto ya se publico bajo ese documento. Espejo de
 * {@code uq_ldv_content} y mapea a 409.
 *
 * <p>
 * Publicar dos veces el mismo contenido crearia dos filas con la misma huella:
 * una aceptacion que apuntara a ese hash dejaria de identificar una version
 * concreta, que es exactamente lo que la huella existe para hacer.
 */
public class LegalDocumentContentAlreadyPublishedException extends RuntimeException {

    public LegalDocumentContentAlreadyPublishedException(String code, String contentHash) {
        super("Legal document " + code + " already has a version with content hash " + contentHash);
    }
}
