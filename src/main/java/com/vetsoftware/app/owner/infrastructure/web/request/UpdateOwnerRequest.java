package com.vetsoftware.app.owner.infrastructure.web.request;

import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOwnerRequest(
    @NotBlank @Size(max = 150) String name,
    @Email @Size(max = 150) String email,
    @NotBlank @Size(max = 50) String document,
    @NotNull OwnerDocumentType documentType,
    @NotNull PersonType personType,
    @Size(max = 1) String verificationDigit,
    @Size(max = 255) String legalName,
    @Size(max = 255) String address,
    @Size(max = 30) String phone,
    @NotNull Long cityId,
    boolean withholdingAgent,
    // Opcional: si no se envía, el backend lo infiere (jurídica/NIT → Responsable de IVA).
    TaxRegime taxRegime,
    // Opcional: si no se envía, el backend usa NO_APLICA (R-99-PN), el caso por defecto.
    FiscalResponsibility fiscalResponsibility) {}
