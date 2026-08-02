package com.vetsoftware.app.electronicdocument.application.command;

/**
 * Transmite a la DIAN (vía el proveedor configurado) un documento ya
 * construido.
 */
public record TransmitElectronicDocumentCommand(Long documentId, Long companyId) {
}
