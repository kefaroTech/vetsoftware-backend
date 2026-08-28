package com.vetsoftware.app.companybillingprofile.domain;

/**
 * Tipo del documento con el que se identifica al tercero que recibe la factura.
 * Dominio cerrado y espejo <strong>literal</strong> de
 * {@code chk_company_billing_profiles_tax_id_kind}.
 *
 * <p>
 * <strong>Es parte de la llave del formato de la informacion exogena</strong>,
 * no un adorno: el mismo numero de documento significa cosas distintas segun
 * venga de una cedula o de un NIT, y reportarlo sin el tipo obliga a deducirlo
 * despues, cliente por cliente.
 *
 * <p>
 * <strong>Este enum no se toca sin un changeset.</strong> Añadir aqui un sexto
 * valor sin ampliar el {@code CHECK} deja la aplicacion compilando y el
 * {@code INSERT} muriendo en produccion con un error que no nombra ni la
 * columna ni el valor.
 */
public enum TaxIdKind {

    /**
     * Numero de Identificacion Tributaria. Es el unico de los cinco que lleva
     * digito de verificacion.
     */
    NIT,

    /** Cedula de ciudadania. */
    CC,

    /** Cedula de extranjeria. */
    CE,

    /** Pasaporte. */
    PASSPORT,

    /** Documento de identificacion expedido en el exterior. */
    FOREIGN_ID
}
