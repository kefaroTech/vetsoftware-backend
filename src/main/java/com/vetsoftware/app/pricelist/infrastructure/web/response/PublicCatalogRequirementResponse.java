package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * «Si eliges esto, se te anade aquello»: un arco del grafo de requisitos del
 * catalogo.
 *
 * <p>
 * <strong>Sin esto, el prospecto solo se entera cuando le rechazan la
 * cotizacion.</strong> El servidor completa el carrito con lo que falta
 * ({@code RequiredItemsClosure}) y rechaza el que mezcla mal
 * ({@code SelfServeCartGuard}), pero hasta hoy el catalogo publico no decia una
 * palabra de las nueve reglas sembradas: un front que montara una cesta no
 * podia avisar de que marcar Facturacion Electronica arrastra Caja.
 *
 * <p>
 * <strong>Arcos directos, no el cierre transitivo.</strong> Ver
 * {@code PublicCatalogRequirementRowDto}: el cierre pierde el porque. Con
 * {@code EXTRA_STORAGE → LAB_IMAGING → CLINICAL_HISTORY} sembrado, el cierre
 * diria que Almacenamiento extra necesita Historia clinica —cierto en el
 * resultado y falso en la causa— y el front no podria escribir la frase que
 * explica la adicion. Quien quiera el cierre lo calcula recorriendo estos arcos
 * en anchura, que es literalmente lo que hace el servidor.
 *
 * <p>
 * <strong>Los dos extremos estan {@code ACTIVE} y habilitados</strong>, mismo
 * predicado que usa el resolvedor del configurador. Un arco hacia un articulo
 * retirado no viaja: exigiria anadir algo que ya no se vende.
 */
public record PublicCatalogRequirementResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Code del articulo que el cliente elige") String itemCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Code del articulo que se le anadira. Puede no aparecer en modules/capacities/oneTimeItems si no tiene precio en la tarifa vigente: el servidor lo anade igual") String requiredItemCode) {
}
