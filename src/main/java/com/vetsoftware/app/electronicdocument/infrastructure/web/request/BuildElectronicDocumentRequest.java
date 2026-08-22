package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import jakarta.validation.constraints.NotNull;

public record BuildElectronicDocumentRequest(
        @NotNull(message = "Debes seleccionar la cuenta abierta.") Long openAccountId,
        @NotNull(message = "Debes seleccionar el tipo de documento electrónico.") ElectronicDocumentType documentType,
        boolean finalConsumer) {
}
