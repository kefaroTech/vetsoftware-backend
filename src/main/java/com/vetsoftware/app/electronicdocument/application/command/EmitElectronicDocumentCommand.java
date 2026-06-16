package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;

/** Emitir end-to-end: construir desde la cuenta cerrada + transmitir + (si valida) entregar. */
public record EmitElectronicDocumentCommand(
        Long openAccountId,
        ElectronicDocumentType documentType,
        Long companyId,
        boolean finalConsumer
) {}
