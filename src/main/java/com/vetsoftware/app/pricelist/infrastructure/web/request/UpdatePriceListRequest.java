package com.vetsoftware.app.pricelist.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * El código no viaja: es la clave de negocio de la lista y no se edita.
 */
public record UpdatePriceListRequest(
        @NotBlank(message = "El nombre de la lista de precios es obligatorio.") @Size(max = 120, message = "El nombre de la lista de precios no puede superar los 120 caracteres.") String name,
        @NotBlank(message = "La moneda es obligatoria.") @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código ISO 4217 de tres letras en mayúsculas.") String currency,
        @NotNull(message = "La fecha de inicio de vigencia es obligatoria.") LocalDate validFrom,
        LocalDate validTo) {
}
