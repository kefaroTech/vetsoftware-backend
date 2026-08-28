package com.vetsoftware.app.legaldocumentversion.domain;

/**
 * No existe la version de texto legal que se pidio. Mapea a 404.
 *
 * <p>
 * El constructor por huella es el que sostiene la prueba del cliente: si el
 * documento que acepto no se puede recuperar por su hash, la aceptacion no
 * demuestra nada, y ese fallo tiene que ser ruidoso y no un texto parecido.
 */
public class LegalDocumentVersionNotFoundException extends RuntimeException {

    public LegalDocumentVersionNotFoundException(Long id) {
        super("Legal document version not found: " + id);
    }

    public LegalDocumentVersionNotFoundException(String code) {
        super("No current legal document version for code: " + code);
    }

    public LegalDocumentVersionNotFoundException(String code, String contentHash) {
        super("No legal document version for code " + code + " with content hash " + contentHash);
    }
}
