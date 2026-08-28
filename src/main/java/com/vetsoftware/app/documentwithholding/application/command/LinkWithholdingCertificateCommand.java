package com.vetsoftware.app.documentwithholding.application.command;

/**
 * Apunta una retencion ya registrada a su certificado.
 *
 * @param id
 *            la retencion que pasa a tener respaldo
 * @param companyId
 *            la empresa duena de las dos filas. No es redundante: las dos
 *            claves foraneas de la tabla son <em>compuestas</em> y comparten la
 *            columna {@code company_id}, asi que un certificado de otra clinica
 *            lo rechazaria la base como error de integridad en vez de como una
 *            peticion invalida
 * @param certificateId
 *            el certificado que la respalda
 */
public record LinkWithholdingCertificateCommand(Long id, Long companyId, Long certificateId) {
}
