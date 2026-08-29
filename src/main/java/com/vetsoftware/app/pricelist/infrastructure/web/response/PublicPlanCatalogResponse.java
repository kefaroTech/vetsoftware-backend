package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * El catalogo comercial publico.
 *
 * <p>
 * {@code priceValidFrom} sale y la fecha de caducidad <strong>no</strong>: con
 * el {@code validTo} publicado, quien compara espera al ultimo dia de la
 * oferta. Tampoco salen el id de la tarifa, su codigo, su estado, quien la
 * publico ni cuando.
 *
 * <p>
 * {@code currency} y {@code priceValidFrom} son nulos y {@code plans} viene
 * vacio cuando no hay tarifa vigente. Es una respuesta 200 valida: «hoy no hay
 * precio publicado» no es un error del cliente ni del servidor, y devolver un
 * 404 dejaria la portada rota por un dato de configuracion.
 */
public record PublicPlanCatalogResponse(
        @Schema(description = "ISO 4217; nulo si no hay tarifa vigente") String currency,
        @Schema(description = "Desde cuando rigen estos precios; nulo si no hay tarifa vigente") LocalDate priceValidFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PublicPlanResponse> plans) {
}
