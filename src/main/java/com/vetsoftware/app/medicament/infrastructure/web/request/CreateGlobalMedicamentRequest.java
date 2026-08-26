package com.vetsoftware.app.medicament.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta en el catalogo global desde la consola de plataforma. Dos campos y
 * ninguno mas: ni {@code companyId} —un recurso de plataforma no tiene empresa,
 * y aceptarla seria dejar que el cliente eligiera de quien es la fila que crea—
 * ni {@code general}, que aqui es una constante del endpoint y no una eleccion.
 */
public record CreateGlobalMedicamentRequest(
        @NotBlank(message = "El nombre del medicamento es obligatorio.") @Size(max = 200, message = "El nombre del medicamento no puede superar los 200 caracteres.") String name,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.") String description) {
}
