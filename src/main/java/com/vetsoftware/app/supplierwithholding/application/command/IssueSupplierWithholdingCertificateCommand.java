package com.vetsoftware.app.supplierwithholding.application.command;

/**
 * Emitir el certificado de retencion que hay que entregarle al proveedor.
 *
 * <p>
 * <strong>Sin fecha</strong>: la pone el caso de uso con su {@code Clock}
 * inyectado. Es un dato probatorio y aceptarlo por HTTP dejaria antedatar una
 * emision.
 *
 * @param certificateRef
 *            el numero del certificado. Es el que el proveedor usa para
 *            descontarse la retencion en <em>su</em> declaracion, asi que una
 *            vez emitido no se reescribe
 */
public record IssueSupplierWithholdingCertificateCommand(Long id, String certificateRef) {
}
