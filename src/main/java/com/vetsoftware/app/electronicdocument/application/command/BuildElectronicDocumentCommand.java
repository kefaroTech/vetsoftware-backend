package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;

/**
 * Construye un documento PENDIENTE desde una cuenta cerrada. companyId lo
 * inyecta el controller.
 */
public record BuildElectronicDocumentCommand(Long openAccountId,
        ElectronicDocumentType documentType, Long companyId, boolean finalConsumer) {
}
