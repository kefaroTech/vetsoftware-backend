package com.vetsoftware.app.pricelist.infrastructure.web.response;

import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Un paquete con su precio y los rotulos de lo que trae dentro.
 *
 * <p>
 * <strong>{@code componentCodes} son rotulos y no objetos anidados a
 * proposito.</strong> El detalle de cada pieza —nombre, precio, prueba— ya
 * viaja una sola vez en {@code modules} y {@code capacities} de la misma
 * respuesta. Anidarlo aqui pondria el mismo dato dos veces en el mismo JSON,
 * con el riesgo clasico: el dia que las dos copias discrepen, no habra forma de
 * saber cual manda.
 *
 * <p>
 * Con esta lista el front puede hacer las dos cuentas que el modelo de compra
 * por necesidad pide: <em>«lo que elegiste cuesta X, este paquete lo incluye
 * por Y»</em>, y <em>«no anadas esta pieza, ya viene en el paquete»</em>. La
 * segunda no es cortesia: el servidor rechaza esa cesta.
 */
public record PublicCatalogPackResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "Descripcion comercial corta del paquete") String tagline,
        @Schema(description = "Precio al mes; nulo si el paquete no esta tarifado en ese ciclo") BigDecimal monthlyAmount,
        @Schema(description = "Precio al ano; nulo si el paquete no esta tarifado en ese ciclo") BigDecimal annualAmount,
        BigDecimal setupAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Rotulos de los articulos que el paquete incluye. Ninguno de ellos se puede comprar ademas del paquete") List<String> componentCodes) {
}
