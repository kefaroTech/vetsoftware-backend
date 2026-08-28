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
 * Declarar el techo de fábrica de un artículo sobre un eje.
 *
 * <p>
 * <strong>No lleva {@code catalogItemId}</strong>: lo pone la ruta. Y no lleva
 * {@code measureKind} porque el command tampoco lo lleva —se resuelve desde el
 * eje—: aceptarlo del cliente permitiría declarar un tipo distinto del real, y
 * aunque la clave foránea compuesta lo mataría en el motor, el error saldría a
 * mitad de una operación de catálogo sin decir qué corregir.
 *
 * <p>
 * <strong>{@code warnThreshold} es {@code Integer} y obligatorio.</strong> Con
 * un {@code int} primitivo, omitirlo en el JSON llegaría al dominio como cero y
 * moriría contra la invariante «entre 1 y 100» con un mensaje que no señala al
 * campo; nulable y {@code @NotNull} responde el error de campo que el front
 * sabe pintar. La columna no declara valor por defecto (changeset 303), así que
 * completarlo aquí sería inventárselo.
 */
public record CreateCatalogItemLimitRequest(
        @NotNull(message = "Debes indicar el eje sobre el que se declara el techo.") Long limitDimensionId,
        @NotNull(message = "Debes indicar si el artículo concede acceso pleno o limitado.") LimitMode mode,
        @PositiveOrZero(message = "La cantidad del techo no puede ser negativa.") Integer limitQuantity,
        ResetPeriod resetPeriod,
        @NotNull(message = "Debes indicar qué pasa cuando se topa el techo.") LimitEnforcement enforcement,
        @Positive(message = "El precio del excedente debe ser mayor que cero.") BigDecimal overageUnitAmount,
        @NotNull(message = "Debes indicar a qué porcentaje avisar.") @Min(value = 1, message = "El porcentaje de aviso debe estar entre 1 y 100.") @Max(value = 100, message = "El porcentaje de aviso debe estar entre 1 y 100.") Integer warnThreshold,
        @NotNull(message = "Debes indicar el techo durante la prueba gratuita.") LimitMode trialMode,
        @PositiveOrZero(message = "La cantidad del techo de prueba no puede ser negativa.") Integer trialLimitQuantity) {
}
