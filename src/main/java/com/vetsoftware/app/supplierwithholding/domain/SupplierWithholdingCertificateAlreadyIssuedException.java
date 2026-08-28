package com.vetsoftware.app.supplierwithholding.domain;

import java.time.LocalDateTime;

/**
 * Se intento emitir un certificado que ya estaba emitido.
 *
 * <p>
 * Es un conflicto (409). <strong>La base no lo impide</strong>:
 * {@code chk_sw_certificate} solo exige que la fecha y la referencia vayan
 * juntas —las dos o ninguna—, asi que una segunda emision pasaria en silencio y
 * machacaria el numero del certificado ya entregado al proveedor. Ese numero es
 * el que el proveedor usa para descontarse la retencion en <em>su</em>
 * declaracion: cambiarlo despues deja dos documentos incompatibles en
 * circulacion y el descuadre aparece en el reporte anual de terceros.
 */
public class SupplierWithholdingCertificateAlreadyIssuedException extends RuntimeException {

    public SupplierWithholdingCertificateAlreadyIssuedException(Long id, LocalDateTime issuedAt) {
        super("Supplier withholding " + id + " already has a certificate issued at " + issuedAt);
    }
}
