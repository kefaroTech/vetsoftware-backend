package com.vetsoftware.app.pricelist.infrastructure.web.response;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Un plan tal como lo ve el mundo.
 *
 * <p>
 * <strong>Sin ningun id.</strong> Ni el del paquete, ni el de la tarifa, ni el
 * de sus articulos: un id es una llave de escritura y un {@code code} es un
 * rotulo. Asi ningun consumidor anonimo puede armar una peticion contra los
 * endpoints de administracion con lo que le devolvio la portada.
 *
 * <p>
 * Los dos importes van rotulados «desde» porque son el tramo de entrada, y
 * cualquiera de los dos puede ser nulo si el paquete solo esta tarifado en un
 * ciclo.
 */
public record PublicPlanResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Descripcion comercial corta del paquete") String tagline,
        BigDecimal monthlyFromAmount, BigDecimal annualFromAmount, BigDecimal setupAmount,
        BigDecimal taxRate, TaxTreatment taxTreatment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PublicPlanIncludedResponse> includes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PublicPlanCapacityResponse> capacities) {
}
