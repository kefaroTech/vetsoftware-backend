package com.vetsoftware.app.companybillingprofile.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El municipio dentro de la ficha de facturacion.
 *
 * <p>
 * <strong>Companion propio y no la {@code CityResponse} de la feature
 * {@code city}</strong>: un {@code web/response} nunca importa el de otra
 * feature.
 *
 * <p>
 * <strong>Los campos son deliberadamente los mismos que los de los otros tres
 * {@code CitySummary} del arbol</strong> —{@code branch}, {@code company} y
 * {@code owner}—: {@code (Long id, String name)}, en ese orden y con la misma
 * obligatoriedad. springdoc nombra los esquemas por el <em>nombre simple</em>,
 * asi que los cuatro records colapsan en un unico
 * {@code #/components/schemas/CitySummary} del contrato. Mientras la forma
 * coincida eso es correcto y es lo que ya hace el repositorio; añadir aqui un
 * campo de mas —el departamento, el codigo DANE— cambiaria el esquema
 * compartido y con el los tipos generados de los dos fronts, sin que nadie
 * tocara {@code branch} ni {@code owner}.
 */
public record CitySummary(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
}
