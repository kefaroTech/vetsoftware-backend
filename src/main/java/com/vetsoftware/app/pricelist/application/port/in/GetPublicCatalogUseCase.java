package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogDto;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Todo lo contratable, tal como lo lee un prospecto que todavia no es cliente.
 *
 * <p>
 * <strong>Por que no es un campo mas de {@link GetPublicPlansUseCase}.</strong>
 * «Planes» son los paquetes y este es el catalogo entero; son dos preguntas y
 * la segunda no es un detalle de la primera. Ademas
 * {@code PublicPlanCatalogResponse} ya lo consume la landing del tenant con
 * {@code MatchesContract}, cuya comprobacion {@code UndeclaredFields} falla en
 * cuanto la respuesta trae un campo que el front no declara: ampliar aquel
 * esquema rompe el build de los dos fronts a cambio de nada, mientras que un
 * recurso nuevo es estrictamente aditivo y no toca una sola linea de lo que ya
 * funciona.
 *
 * <p>
 * <strong>Hacer publica una ruta en este proyecto son DOS cosas, no
 * una.</strong> Esta anotacion es la primera; la segunda es
 * {@code new Route(HttpMethod.GET, "/catalog")} en
 * {@code PublicRoutes.BUSINESS}, con patron literal y <strong>nunca</strong> un
 * comodin — es el mismo razonamiento que dejaron {@code /configurator} y
 * {@code /plans} con sus rutas exactas. Sin la linea de la ruta, el
 * {@code AuthFilter} rechaza la peticion con un 401 antes de llegar aqui y la
 * anotacion no se llega a mirar; sin la anotacion,
 * {@code PUERTO_CON_PREAUTHORIZE} rompe el build.
 *
 * <p>
 * Es un {@code GET}, asi que no le aplica la invariante de
 * {@code LoginRateLimitFilterTest} sobre toda ruta publica {@code POST}.
 *
 * <p>
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} no aplica por la misma razon que en
 * {@link GetPublicPlansUseCase}: {@code catalog_items}, {@code catalog_prices}
 * y {@code bundle_components} son catalogo global de plataforma y no tienen
 * {@code company_id}, asi que
 * {@link com.vetsoftware.app.pricelist.application.port.out.PublicCatalogQueryPort}
 * no declara —ni puede declarar— ningun metodo que filtre por empresa.
 */
@NoAuthorizationRequired(reason = "Lo lee el configurador de la landing comercial: quien lo consulta es un prospecto sin cuenta que esta decidiendo que modulos necesita, y exigir token haria imposible ver un precio antes de ser cliente. No devuelve dato alguno de ninguna empresa -las tres tablas del catalogo comercial no tienen company_id-, es de solo lectura, y su DTO es una proyeccion pobre que omite ids, estados, la escalera de tramos y todo lo que solo edita SYSTEM.")
public interface GetPublicCatalogUseCase {

    /**
     * Lo contratable de la tarifa vigente hoy: modulos, contadores, cargos unicos y
     * paquetes, cada uno con su precio en los dos ciclos.
     *
     * <p>
     * Catalogo vacio —con la moneda nula— si no hay tarifa vigente. Es una
     * situacion normal del catalogo, no un error que deba tumbar la portada, y es
     * la misma decision que toma {@link GetPublicPlansUseCase#get()}.
     */
    PublicCatalogDto get();
}
