package com.vetsoftware.app.pricelist.infrastructure.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Todo lo contratable de la tarifa vigente, para que un configurador pueda
 * poner precio a cualquier combinacion.
 *
 * <p>
 * <strong>Que tiene esto que no tenga
 * {@code PublicPlanCatalogResponse}.</strong> Aquel publica los paquetes y,
 * dentro de cada uno, sus modulos <em>sin precio</em>: sirve para elegir un
 * plan cerrado. Este publica el precio de cada articulo por si mismo, que es lo
 * que hace falta para que el cliente componga lo que necesita. Los dos
 * conviven; ninguno sustituye al otro.
 *
 * <p>
 * <strong>{@code priceValidFrom} sale y la fecha de caducidad no</strong>, por
 * lo mismo que en el otro: con el {@code validTo} publicado, quien compara
 * espera al ultimo dia de la oferta. Tampoco sale el id de la tarifa, ni su
 * codigo, ni su estado.
 *
 * <p>
 * {@code currency} y {@code priceValidFrom} nulos con todas las listas vacias
 * es una respuesta 200 valida: «hoy no hay precio publicado» no es un error del
 * cliente ni del servidor, y un 404 dejaria la portada rota por un dato de
 * configuracion. {@code areas} tambien viaja vacia ahi aunque el area no
 * dependa de la tarifa: sin modulos que agrupar, una cabecera es un titulo
 * sobre la nada.
 *
 * <p>
 * <strong>{@code requirements} va aqui arriba y no dentro de cada
 * articulo</strong>, y es una decision de contrato antes que de ergonomia. Los
 * tres records de articulo —{@link PublicCatalogItemResponse},
 * {@link PublicCatalogCapacityResponse}, {@link PublicCatalogPackResponse}— se
 * quedan <em>sin tocar</em>, asi que los dos fronts declaran un esquema nuevo
 * en vez de reabrir tres que ya funcionan y que sus pruebas de contrato ya
 * afirman. Y un arco es un arco: colgarlo del articulo obligaria a repetir el
 * campo en los tres y a decidir donde va un requisito con origen en un
 * {@code BUNDLE}, que hoy no existe y el dia que exista se perderia en
 * silencio.
 */
public record PublicCatalogResponse(
        @Schema(description = "ISO 4217; nulo si no hay tarifa vigente") String currency,
        @Schema(description = "Desde cuando rigen estos precios; nulo si no hay tarifa vigente") LocalDate priceValidFrom,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Funcionalidades que se encienden, con su precio suelto") List<PublicCatalogItemResponse> modules,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Contadores que se compran por unidades") List<PublicCatalogCapacityResponse> capacities,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cargos unicos con precio de lista; se cotizan con un comercial, no por autoservicio") List<PublicCatalogItemResponse> oneTimeItems,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Paquetes, con su precio y su composicion") List<PublicCatalogPackResponse> packs,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Grafo de requisitos: arcos DIRECTOS «si eliges itemCode, se anade requiredItemCode». No es el cierre transitivo; recorrelos en anchura si lo necesitas") List<PublicCatalogRequirementResponse> requirements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cabeceras funcionales, YA ORDENADAS: el orden de la lista es el de presentacion y no se reordena en el cliente. Vacia si no hay tarifa vigente") List<PublicCatalogAreaResponse> areas) {
}
