package com.vetsoftware.app.accountingaccount.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo unico editable de una cuenta publicada. Sin {@code id} —lo lleva la ruta—
 * y sin codigo, clase, nivel ni padre: cambiar cualquiera de esos cuatro
 * reescribiria el significado de los asientos que ya apuntan a la cuenta.
 */
public record UpdateAccountingAccountRequest(
        @NotBlank(message = "Debes indicar el nombre de la cuenta.") @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.") String name,
        @NotNull(message = "Debes indicar si la cuenta exige tercero identificado.") Boolean requiresThirdParty) {
}
