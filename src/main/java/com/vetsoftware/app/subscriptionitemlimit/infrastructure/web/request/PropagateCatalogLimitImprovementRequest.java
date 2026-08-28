package com.vetsoftware.app.subscriptionitemlimit.infrastructure.web.request;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Propagar a los contratos vivos una <em>mejora</em> del techo de fábrica
 * (D-75).
 *
 * <p>
 * <strong>Cruza todas las empresas por definición</strong>, así que no lleva
 * —ni puede llevar— {@code companyId} en ninguna forma: su puerto está cerrado
 * a {@code hasRole('SYSTEM')} a secas y ningún principal de tenant lo alcanza.
 *
 * <p>
 * Solo mejoras: subir el cupo de 100 a 200 llega a los cuarenta contratos
 * vivos; bajarlo de 100 a 80 no toca ninguno. El filtro lo aplica el dominio en
 * {@code SubscriptionItemLimit.improveFrom}, no esta frontera.
 */
public record PropagateCatalogLimitImprovementRequest(
        @NotNull(message = "Debes indicar el artículo cuyo techo mejoró.") Long catalogItemId,
        @NotNull(message = "Debes indicar el eje cuyo techo mejoró.") Long limitDimensionId,
        @NotNull(message = "Debes indicar el modo del techo de fábrica.") LimitMode factoryMode,
        @PositiveOrZero(message = "La cantidad del techo de fábrica no puede ser negativa.") Integer factoryLimitQuantity) {
}
