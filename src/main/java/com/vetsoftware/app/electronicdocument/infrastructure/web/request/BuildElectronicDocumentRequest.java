package com.vetsoftware.app.electronicdocument.infrastructure.web.request;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import jakarta.validation.constraints.NotNull;

public record BuildElectronicDocumentRequest(
        @NotNull Long openAccountId,
        @NotNull ElectronicDocumentType documentType
) {}
