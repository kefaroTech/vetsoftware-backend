package com.vetsoftware.app.catalogitemlimit.infrastructure.web.request;

import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.ResetPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Cambiar el techo de fábrica.
 *
 * <p>
 * <strong>Ni el eje ni el tipo de medida son editables</strong>, y por eso no
 * están aquí: el eje es parte de la identidad de la fila —cambiarlo es declarar
 * otro techo— y el tipo de medida va atado por clave foránea compuesta contra
 * {@code limit_dimensions(id, measure_kind)}.
 *
 * <p>
 * Cambiar esto <strong>no cambia nada a quien ya firmó</strong>: los contratos
 * vivos leen su copia congelada. Propagar una mejora a los contratos vivos es
 * otra operación, deliberadamente separada (D-75), y vive en
 * {@code subscriptionitemlimit}.
 */
public record UpdateCatalogItemLimitRequest(
        @NotNull(message = "Debes indicar si el artículo concede acceso pleno o limitado.") LimitMode mode,
        @PositiveOrZero(message = "La cantidad del techo no puede ser negativa.") Integer limitQuantity,
        ResetPeriod resetPeriod,
        @NotNull(message = "Debes indicar qué pasa cuando se topa el techo.") LimitEnforcement enforcement,
        @Positive(message = "El precio del excedente debe ser mayor que cero.") BigDecimal overageUnitAmount,
        @NotNull(message = "Debes indicar a qué porcentaje avisar.") @Min(value = 1, message = "El porcentaje de aviso debe estar entre 1 y 100.") @Max(value = 100, message = "El porcentaje de aviso debe estar entre 1 y 100.") Integer warnThreshold,
        @NotNull(message = "Debes indicar el techo durante la prueba gratuita.") LimitMode trialMode,
        @PositiveOrZero(message = "La cantidad del techo de prueba no puede ser negativa.") Integer trialLimitQuantity) {
}
