package com.vetsoftware.app.platformtaxprofile.domain;

/**
 * Con que documento se identifica Lumbre ante la DIAN.
 *
 * <p>
 * <strong>La base NO impone esta lista.</strong> {@code document_type} es un
 * {@code VARCHAR(30)} sin {@code CHECK}, y el changeset 367 lo dejo asi a
 * proposito: «el dominio Java decide esos valores, y esta migracion no inventa
 * uno que el Java no comparta todavia». Es decir, este enum <em>es</em> la
 * unica barandilla; una fila escrita por fuera de la aplicacion con
 * {@code 'RUT'} entraria sin que nadie se queje y reventaria al leerla, con un
 * {@code IllegalArgumentException} de {@code valueOf} en mitad de la emision de
 * una factura.
 *
 * <p>
 * <strong>Los cuatro valores y sus codigos DIAN son los mismos que
 * {@code companytaxprofile.domain.CompanyDocumentType}, literalmente y a
 * proposito.</strong> Aquella describe con que documento se identifica una
 * clinica que emite; esta, con cual se identifica Lumbre. El hecho del mundo es
 * el mismo —la tabla 3.1 de la resolucion de facturacion electronica—, asi que
 * un tercer vocabulario para el mismo concepto seria exactamente la divergencia
 * silenciosa que el documento de esquema persigue: el dia que alguien cotejara
 * las dos columnas, {@code NIT} y {@code Nit} no cuadrarian y nadie sabria cual
 * de las dos esta mal.
 *
 * <p>
 * <strong>Por que es un enum propio y no el de
 * {@code companytaxprofile}.</strong> El dominio de una feature nunca importa
 * el dominio de otra. La duplicacion es el precio declarado del vertical
 * slicing y lo que compra es que la consola de plataforma pueda cambiar su
 * vocabulario sin tocar la ficha fiscal de ninguna clinica.
 */
public enum PlatformDocumentType {

    NIT(31), CEDULA_CIUDADANIA(13), CEDULA_EXTRANJERIA(22), PASAPORTE(41);

    private final int dianCode;

    PlatformDocumentType(int dianCode) {
        this.dianCode = dianCode;
    }

    /** El codigo de la tabla 3.1 de la DIAN, que es lo que viaja en el XML. */
    public int dianCode() {
        return dianCode;
    }

    /**
     * {@code true} si el documento lleva digito de verificacion. Solo el NIT lo
     * tiene, y de eso depende la mitad de
     * {@code PlatformTaxProfile#validateVerificationDigit}.
     */
    public boolean hasVerificationDigit() {
        return this == NIT;
    }
}
