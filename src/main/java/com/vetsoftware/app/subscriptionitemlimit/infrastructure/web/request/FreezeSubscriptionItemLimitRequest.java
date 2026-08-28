package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.request;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.ResetPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Congelar en la línea del contrato el techo que regía el día de la firma.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: lo prohíbe
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} y aquí entra por la ruta
 * ({@code /system/subscription-item-limits/companies/{companyId}}), que es la
 * salida que la propia regla declara para un gate de plataforma —el principal
 * es cross-tenant por diseño y elegir empresa es justo lo que tiene que poder
 * hacer—.
 *
 * <p>
 * <strong>{@code measureKind} sí viaja aquí</strong>, al revés que en el techo
 * de fábrica: esta fila es una <em>copia congelada</em> y quien la escribe es
 * el alta comercial, que ya resolvió el eje. El motor la ata igualmente contra
 * {@code limit_dimensions(id, measure_kind)}.
 */
public record FreezeSubscriptionItemLimitRequest(
        @NotNull(message = "Debes indicar la línea del contrato.") Long subscriptionItemId,
        @NotNull(message = "Debes indicar el eje sobre el que se congela el techo.") Long limitDimensionId,
        @NotNull(message = "Debes indicar cómo se mide el eje.") MeasureKind measureKind,
        @NotNull(message = "Debes indicar si la línea concede acceso pleno o limitado.") LimitMode mode,
        @PositiveOrZero(message = "La cantidad del techo no puede ser negativa.") Integer limitQuantity,
        ResetPeriod resetPeriod,
        @NotNull(message = "Debes indicar qué pasa cuando se topa el techo.") LimitEnforcement enforcement,
        @Positive(message = "El precio del excedente debe ser mayor que cero.") BigDecimal overageUnitAmount,
        @NotNull(message = "Debes indicar a qué porcentaje avisar.") @Min(value = 1, message = "El porcentaje de aviso debe estar entre 1 y 100.") @Max(value = 100, message = "El porcentaje de aviso debe estar entre 1 y 100.") Integer warnThreshold) {
}
