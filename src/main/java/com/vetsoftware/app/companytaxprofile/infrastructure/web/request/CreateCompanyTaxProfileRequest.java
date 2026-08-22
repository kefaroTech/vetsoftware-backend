package com.vetsoftware.app.companytaxprofile.infrastructure.web.request;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCompanyTaxProfileRequest(
        @NotNull(message = "Debes seleccionar el tipo de documento.") CompanyDocumentType documentType,
        @NotBlank(message = "El número de documento de la empresa es obligatorio.") @Size(max = 20, message = "El número de documento de la empresa no puede superar los 20 caracteres.") String companyDocumentId,
        @Size(max = 1, message = "El dígito de verificación es un solo carácter.") String companyDocumentVerificationDigit,
        @NotBlank(message = "La razón social es obligatoria.") @Size(max = 255, message = "La razón social no puede superar los 255 caracteres.") String legalName,
        @NotNull(message = "Debes seleccionar el régimen tributario.") TaxRegime taxRegime,
        @NotBlank(message = "El correo electrónico fiscal es obligatorio.") @Size(max = 255, message = "El correo electrónico fiscal no puede superar los 255 caracteres.") String fiscalEmail,
        @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres.") String commercialName,
        Long economicActivityId,
        List<@NotBlank(message = "El código de responsabilidad fiscal es obligatorio.") @Size(max = 10, message = "El código de responsabilidad fiscal no puede superar los 10 caracteres.") String> responsibilities) {
}
