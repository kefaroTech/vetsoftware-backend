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
        @NotBlank(message = "El nombre del propietario es obligatorio.") @Size(max = 150, message = "El nombre del propietario no puede superar los 150 caracteres.") String name,
        @Email(message = "El correo electrónico no tiene un formato válido.") @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres.") String email,
        @NotBlank(message = "El número de documento es obligatorio.") @Size(max = 50, message = "El número de documento no puede superar los 50 caracteres.") String document,
        @NotNull(message = "Debes seleccionar el tipo de documento.") OwnerDocumentType documentType,
        @NotNull(message = "Debes seleccionar el tipo de persona.") PersonType personType,
        @Size(max = 1, message = "El dígito de verificación es un solo carácter.") String verificationDigit,
        @Size(max = 255, message = "La razón social no puede superar los 255 caracteres.") String legalName,
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.") String address,
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.") String phone,
        @NotNull(message = "Debes seleccionar la ciudad.") Long cityId, boolean withholdingAgent,
        // Opcional: si no se envía, el backend lo infiere (jurídica/NIT → Responsable
        // de IVA).
        TaxRegime taxRegime,
        // Opcional: si no se envía, el backend usa NO_APLICA (R-99-PN), el caso por
        // defecto.
        FiscalResponsibility fiscalResponsibility) {
}
