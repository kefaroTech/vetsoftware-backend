package com.vetsoftware.app.pricelist.infrastructure.web.response;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Un contador comprado suelto.
 *
 * <p>
 * <strong>No es {@link PublicPlanCapacityResponse}</strong>, y la diferencia
 * importa: alli {@code included} son las unidades que trae <em>un paquete</em>
 * ({@code bundle_components.quantity}); aqui son las que trae el propio tramo
 * de entrada del articulo ({@code catalog_prices.included_quantity}), que es la
 * cifra que resta el motor de precios antes de repartir por tramos. Quien
 * compra sin paquete necesita la segunda; quien compra un paquete, la primera.
 *
 * <p>
 * Van dos, una por ciclo, porque {@code included_quantity} es columna de la
 * fila de precio y hay una fila por ciclo. Nada obliga a que coincidan, y
 * suponer que si es como se publica una cifra que la factura desmiente.
 *
 * <p>
 * Solo el <strong>tramo de entrada</strong>. La escalera completa es la
 * politica de descuento por volumen y publicarla entera es publicarla.
 */
public record PublicCatalogCapacityResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Descripcion comercial corta") String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Parte del minimo estructural (catalog_items.is_core)") boolean mandatory,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Codigo del eje: USER, BRANCH...") String unit,
        @Schema(description = "Unidades que trae el tramo de entrada mensual; nulo si no hay tramo mensual") Integer monthlyIncludedQuantity,
        @Schema(description = "Unidades que trae el tramo de entrada anual; nulo si no hay tramo anual") Integer annualIncludedQuantity,
        @Schema(description = "Precio de la unidad al mes; nulo si no se vende suelta en ese ciclo") BigDecimal monthlyUnitAmount,
        @Schema(description = "Precio de la unidad al ano; nulo si no se vende suelta en ese ciclo") BigDecimal annualUnitAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean selfServiceEligible) {
}
