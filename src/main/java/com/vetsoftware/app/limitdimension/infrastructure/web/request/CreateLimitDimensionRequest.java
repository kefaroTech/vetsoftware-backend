package com.vetsoftware.app.limitdimension.infrastructure.web.request;

import com.vetsoftware.app.limitdimension.domain.MeasureKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Declarar un eje limitable.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>, y aquí sale gratis: el catálogo de
 * ejes no pertenece a ninguna empresa. Es lo que exige
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}.
 *
 * <p>
 * Las restricciones de aquí son de <em>forma</em> —lo que el binder puede
 * rechazar sin saber nada del negocio— y espejan los máximos del dominio. La
 * regla cruzada que ata {@code releaseDelayDays} a un eje {@code CUMULATIVE}
 * <strong>no</strong> se declara aquí: vive en el constructor de
 * {@code LimitDimension}, que es donde el CLAUDE.md pone las invariantes, y
 * duplicarla en la frontera sería tener dos sitios que pueden divergir.
 */
public record CreateLimitDimensionRequest(
        @NotBlank(message = "El código del eje es obligatorio.") @Size(max = 50, message = "El código del eje no puede superar los 50 caracteres.") String code,
        @NotBlank(message = "El nombre del eje es obligatorio.") @Size(max = 120, message = "El nombre del eje no puede superar los 120 caracteres.") String name,
        @NotNull(message = "Debes indicar cómo se mide el eje.") MeasureKind measureKind,
        Long subModuleId,
        @PositiveOrZero(message = "Los días de enfriamiento no pueden ser negativos.") Integer releaseDelayDays,
        @NotNull(message = "Debes indicar desde cuándo está disponible el eje.") LocalDate availableFrom) {
}
