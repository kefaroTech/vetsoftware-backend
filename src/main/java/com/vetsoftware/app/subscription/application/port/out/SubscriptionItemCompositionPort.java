package com.vetsoftware.app.subscription.application.port.out;

import java.util.List;

/**
 * Congela la composicion de un articulo en el momento de firmar la linea (D-76,
 * R-CAT-03).
 *
 * <p>
 * <b>Que problema cierra.</b> Hasta hoy el recalculo de permisos resolvia la
 * composicion EN VIVO: cruzaba la linea del contrato contra
 * {@code catalog_item_sub_modules} y {@code bundle_components} tal y como
 * estuvieran en el instante del recalculo. Quitar un submodulo del catalogo se
 * lo quitaba a todas las clinicas que lo pagan —sin otrosi, sin aviso y sin
 * bajarles el precio— y ni siquiera en solo lectura, porque la degradacion solo
 * alcanza a lo que la consulta devuelve. Con un agravante de reloj: ningun
 * cambio de catalogo dispara recalculo, asi que las cuarenta clinicas no lo
 * pierden el dia del cambio, lo pierden de una en una meses despues, cada una
 * el dia que le toque recalculo por cualquier otro motivo. Un incidente sin
 * fecha y sin correlacion con su causa.
 *
 * <p>
 * Lo que el cliente compro es lo que el articulo tenia el dia que firmo, igual
 * que el precio y que el cupo.
 */
public interface SubscriptionItemCompositionPort {

    /**
     * Escribe la foto de la composicion del articulo para esa linea recien abierta.
     *
     * <p>
     * Expande los paquetes: un {@code BUNDLE} no ata submodulos por si mismo, los
     * ata a traves de sus componentes, y congelar solo el primer nivel dejaria al
     * cliente de un plan empaquetado sin un solo permiso. Idempotente por
     * {@code uq_subscription_item_sub_modules}: reescribir la misma foto no
     * duplica.
     *
     * <p>
     * ⛔ <b>Una linea que no es de esta empresa LANZA, no devuelve cero.</b> La
     * pertenencia la impone la clave foranea compuesta
     * {@code (company_id, subscription_item_id)} de la tabla de composicion, y el
     * adaptador ya no la silencia: hasta el arreglo de esta sesion la sentencia era
     * {@code INSERT IGNORE}, que degradaba esa violacion a un aviso y devolvia
     * cero, indistinguible del cero legitimo de abajo — con los dos llamantes
     * descartando el valor de retorno, una firma cruzada entre clinicas quedaba sin
     * foto y por tanto sin permisos, en silencio y para siempre.
     *
     * @return cuantos submodulos quedaron congelados. Cero es legitimo <b>en un
     *         solo caso</b> —una linea de capacidad no ata ninguno— y en la segunda
     *         pasada sobre una foto ya escrita, que es la idempotencia. Cualquier
     *         otro fallo se propaga como excepcion.
     */
    int freeze(Long companyId, Long subscriptionItemId, Long catalogItemId);

    /**
     * Los submodulos congelados de una linea. Solo lectura, para comprobaciones.
     */
    List<Long> findFrozenSubModuleIds(Long companyId, Long subscriptionItemId);
}
