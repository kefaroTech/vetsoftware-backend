package com.vetsoftware.app.legaldocumentversion.domain;

/**
 * Se pidio suceder una version que ya estaba sucedida. Mapea a 409.
 *
 * <p>
 * Aceptarlo moveria {@code superseded_at} a una fecha posterior y falsearia
 * desde cuando dejo de regir ese texto —la pregunta exacta que se hace quien
 * revisa que version estaba vigente el dia de una aceptacion—.
 */
public class LegalDocumentVersionAlreadySupersededException extends RuntimeException {

    public LegalDocumentVersionAlreadySupersededException(String code, int documentVersion) {
        super("Legal document " + code + " version " + documentVersion + " is already superseded");
    }
}
