package com.vetsoftware.app.aiproposal.domain;

/**
 * Companion VO de {@code legal_document_versions}: lo unico que esta rodaja
 * necesita saber de un texto legal.
 *
 * <p>
 * Ni el contenido, ni el hash, ni quien lo publico. Esta rodaja responde dos
 * preguntas -<em>que version exacta se le enseno</em>, que es la FK
 * {@code privacy_notice_version_id}, y <em>cual acepto</em>, que es la fila de
 * {@code legal_document_acceptances}- y para las dos basta el id, el codigo y
 * si el documento es el aviso que ampara la recogida.
 *
 * @param privacyNotice
 *            si este documento es el que ampara el tratamiento del correo y del
 *            texto libre. Lo resuelve el adaptador con {@code kind} y no con el
 *            {@code code}, que es editorial y lo cambia negocio
 */
public record LegalDocumentVersionRef(Long id, String code, int documentVersion,
        boolean privacyNotice) {

    public LegalDocumentVersionRef {
        if (id == null)
            throw new IllegalArgumentException("legal document version id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("legal document code is required");
        if (documentVersion < 1)
            throw new IllegalArgumentException("documentVersion must be at least 1: " + code);
    }
}
