package com.vetsoftware.app.legaldocumentversion.domain;

/**
 * Que clase de texto legal es. Espejo <strong>literal</strong> de
 * {@code chk_ldv_kind}: los cinco nombres se escriben aqui igual que en la
 * constraint porque {@code @Enumerated(EnumType.STRING)} guarda el
 * {@code name()} tal cual.
 */
public enum LegalDocumentKind {

    /** Terminos y condiciones del servicio. */
    TERMS,

    /** Politica de tratamiento de datos personales. */
    PRIVACY_POLICY,

    /** Contrato de encargo del tratamiento con el responsable. */
    DATA_PROCESSING_AGREEMENT,

    /** Aviso de privacidad. */
    PRIVACY_NOTICE,

    /** Anexo de compromisos. */
    COMMITMENT_ANNEX
}
