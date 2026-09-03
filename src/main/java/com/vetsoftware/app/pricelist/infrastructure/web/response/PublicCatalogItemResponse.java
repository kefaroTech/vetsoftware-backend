package com.vetsoftware.app.pricelist.infrastructure.web.response;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Un articulo que se compra suelto: un modulo, o un cargo unico.
 *
 * <p>
 * <strong>Sin ningun id</strong>, por lo mismo que {@link PublicPlanResponse}:
 * un id es una llave de escritura y un {@code code} es un rotulo. La
 * autocontratacion nombra los articulos por {@code code} justamente para que la
 * portada no tenga que publicar ninguna llave.
 *
 * <p>
 * <strong>Nulo en un importe significa «no se vende en ese ciclo».</strong> No
 * es un hueco ni un cero: es el mismo predicado que la contratacion va a
 * evaluar, porque {@code JpaPublishedCatalogItemQueryPort} exige precio de
 * entrada ({@code tier_min = 1}) en el ciclo pedido con un {@code JOIN}
 * interno. Quien pinta esto no debe ofrecer el articulo para ese ciclo.
 */
public record PublicCatalogItemResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Descripcion comercial corta") String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Parte del minimo estructural (catalog_items.is_core): el alta de toda empresa lo incluye y no se puede desmarcar") boolean mandatory,
        @Schema(description = "Dias de prueba del articulo; nulo si su politica no concede prueba") Integer trialDays,
        @Schema(description = "Precio al mes; nulo si no se vende suelto en ese ciclo") BigDecimal monthlyAmount,
        @Schema(description = "Precio al ano; nulo si no se vende suelto en ese ciclo. No es el mensual por doce") BigDecimal annualAmount,
        @Schema(description = "Cargo unico de puesta en marcha. En un articulo ONE_TIME es TODO su precio: DATA_MIGRATION vale 0.00 por ciclo y 450000.00 aqui") BigDecimal setupAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Si la autocontratacion lo aceptaria como linea. Falso en los cargos unicos, que se negocian") boolean selfServiceEligible,
        @Schema(description = "Codigo del area funcional bajo cuya cabecera va el modulo; casa con areas[].code. Nulo en todo articulo que no se agrupa bajo una cabecera: los cargos unicos, cualquiera que no sea MODULE, y tambien CORE, que se pinta en una fila fija sobre las cabeceras") String areaCode,
        @Schema(description = "Rotulo corto para la casilla, mas breve que name y distinto de description. Nulo mientras no se haya escrito: en ese caso se pinta name") String shortLabel) {
}
