package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.PublicPlanCatalogDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Los planes tal como los lee un prospecto que todavia no es cliente.
 *
 * <p>
 * Es la unica lectura anonima del slice y vive <strong>separado</strong> de los
 * puertos de administracion a proposito, como
 * {@code GetPublicQuestionnaireUseCase} en {@code configurator}: mezclar «lo
 * que puede ver el mundo» con «lo que puede editar SYSTEM» en un mismo puerto
 * convierte cualquier campo nuevo del lado de administracion en una fuga
 * silenciosa hacia la respuesta publica. Por eso devuelve
 * {@link PublicPlanCatalogDto} y no {@code PriceListDto} ni
 * {@code CatalogPriceDto}: la forma publica es mas pobre por diseno.
 *
 * <p>
 * <strong>Hacer publica una ruta en este proyecto son DOS cosas, no
 * una.</strong> Esta anotacion es la primera; la segunda es
 * {@code new Route(HttpMethod.GET, "/plans")} en {@code PublicRoutes.BUSINESS},
 * con patron literal y <strong>nunca</strong> un comodin sobre el prefijo: el
 * mismo prefijo acabara colgando la administracion de planes y un comodin la
 * abriria al mundo sin que nadie lo vea en el diff. Sin la ruta, el
 * {@code AuthFilter} rechaza la peticion con un 401 antes de llegar aqui, que
 * es el fallo exacto que documenta {@code ResolveConfiguratorSelectionUseCase}.
 *
 * <p>
 * Es un {@code GET}, asi que no le aplica la invariante de
 * {@code LoginRateLimitFilterTest} sobre toda ruta publica {@code POST}.
 *
 * <p>
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} no aplica, y no por la exencion de
 * la anotacion: {@code price_lists}, {@code catalog_items} y
 * {@code catalog_prices} son catalogo global de plataforma y no tienen
 * {@code company_id}, asi que
 * {@link com.vetsoftware.app.pricelist.application.port.out.PublicPlanQueryPort}
 * no declara —ni puede declarar— ningun metodo que filtre por empresa.
 */
@NoAuthorizationRequired(reason = "Lo lee la landing comercial: quien lo consulta es un prospecto sin cuenta, y exigir token haria imposible publicar un precio antes de ser cliente. No devuelve dato alguno de ninguna empresa -las tres tablas del catalogo comercial no tienen company_id-, es de solo lectura, y su DTO es una proyeccion pobre que omite ids, estados, tramos y todo lo que solo edita SYSTEM.")
public interface GetPublicPlansUseCase {

    /**
     * Los planes de la tarifa vigente hoy. Catalogo vacio si no hay tarifa vigente
     * o si todavia no hay ningun paquete tarifado: las dos son situaciones normales
     * del catalogo, no errores que deban tumbar la portada.
     */
    PublicPlanCatalogDto get();
}
