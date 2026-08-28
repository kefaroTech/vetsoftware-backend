package com.vetsoftware.app.supplierwithholding.domain;

/**
 * Que documento identifica al proveedor. Dominio cerrado y <strong>espejo
 * exacto</strong> de {@code chk_sw_doc_type}, con el mismo vocabulario que
 * {@code company_billing_profiles.tax_id_kind}.
 *
 * <p>
 * <strong>El nombre simple es distinto del {@code TaxIdKind} de
 * {@code companybillingprofile} a proposito</strong>: springdoc funde los
 * esquemas del contrato por nombre simple, y dos enums homonimos que hoy
 * coinciden son una trampa esperando a que uno de los dos crezca.
 *
 * <p>
 * Sin el documento no se puede armar el reporte anual de terceros, que se hace
 * con el numero y no con el nombre.
 */
public enum SupplierDocumentKind {

    /** Numero de identificacion tributaria. Lo normal en un proveedor. */
    NIT,

    /** Cedula de ciudadania. */
    CC,

    /** Cedula de extranjeria. */
    CE,

    /** Pasaporte. */
    PASSPORT,

    /** Documento de identificacion del exterior. */
    FOREIGN_ID
}
