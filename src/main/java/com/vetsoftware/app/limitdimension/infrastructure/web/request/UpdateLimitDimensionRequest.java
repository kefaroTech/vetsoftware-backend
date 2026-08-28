package com.vetsoftware.app.limitdimension.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Editar un eje limitable.
 *
 * <p>
 * <strong>Sin código, sin tipo de medida y sin fecha de
 * disponibilidad.</strong> No es que se hayan olvidado: los tres son identidad
 * o hecho, no atributos. El código es la clave con la que la línea del contrato
 * nombra el eje y se cruza viva en cada recálculo; el tipo va atado por clave
 * foránea compuesta desde los techos vendidos; y la fecha decide D-74, así que
 * moverla cambia el techo de contratos ya firmados. Que no estén aquí es lo que
 * impide pedirlos.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>, y aquí sale gratis: el catálogo de
 * ejes no pertenece a ninguna empresa. Es lo que exige
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}.
 *
 * <p>
 * Las restricciones son de <em>forma</em> y espejan los máximos del dominio. La
 * regla cruzada que ata {@code releaseDelayDays} a un eje {@code CUMULATIVE}
 * <strong>no</strong> se declara aquí: vive en el constructor de
 * {@code LimitDimension}, y duplicarla en la frontera sería tener dos sitios
 * que pueden divergir.
 */
public record UpdateLimitDimensionRequest(
        @NotBlank(message = "El nombre del eje es obligatorio.") @Size(max = 120, message = "El nombre del eje no puede superar los 120 caracteres.") String name,
        Long subModuleId,
        @PositiveOrZero(message = "Los días de enfriamiento no pueden ser negativos.") Integer releaseDelayDays) {
}
