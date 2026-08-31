package com.vetsoftware.app.catalogitemaihint.application.port.out;

import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Resuelve el articulo del catalogo sin que nada fuera de
 * {@code infrastructure/persistence} conozca la feature {@code catalogitem}.
 *
 * <p>
 * <strong>Un solo puerto para las dos cosas</strong> —validar que el articulo
 * existe antes de publicar, y traer codigo y nombre para pintarlos—, a
 * diferencia de {@code pricelist}, que separa {@code CatalogItemQueryPort} de
 * {@code CatalogItemValidationPort}. Alli la division se paga sola porque la
 * guarda de escritura corre en un camino que no necesita ninguna columna; aqui
 * publicar es una operacion de una sola fila cuya respuesta ya lleva el codigo
 * y el nombre, asi que partirlo obligaria a leer el mismo articulo dos veces
 * por alta.
 *
 * <p>
 * &#9940; <strong>Sus dos metodos NO filtran igual, y hay que leerlos por
 * separado.</strong> {@link #findById} es la guarda que decide si se puede
 * publicar y exige articulo a la venta; {@link #findAllByIds} solo resuelve
 * nombres para pintar y se conforma con que la fila este habilitada. Cada uno
 * dice su criterio en su propio Javadoc.
 *
 * <p>
 * <strong>Sin variante acotada por empresa</strong>: {@code catalog_items} es
 * catalogo global de plataforma y no tiene {@code company_id}, asi que
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica —no hay
 * empresa que ofrecer—.
 */
public interface CatalogItemQueryPort {

    /**
     * <strong>La guarda de escritura.</strong> El articulo, solo si existe, esta
     * <b>a la venta</b> ({@code status = 'ACTIVE'}) y habilitado. Vacio para
     * cualquier otra cosa: publicarle una pista a un articulo en borrador o
     * retirado le ensenaria al modelo un codigo que el motor rechazara despues, y
     * el unico que se entera es el prospecto cuando su cotizacion falla.
     */
    Optional<CatalogItemRef> findById(Long catalogItemId);

    /**
     * <strong>El pintado del listado, y NO usa el mismo criterio que
     * {@link #findById}.</strong> Devuelve los articulos pedidos que existan y
     * esten habilitados, <b>sin exigir {@code ACTIVE}</b>, indexados por id.
     *
     * <p>
     * La diferencia es deliberada y esta medida en las dos direcciones: aqui no se
     * autoriza nada, solo se le pone codigo y nombre a filas del historial que ya
     * existen. La pista de un articulo retirado sigue siendo una fila legitima
     * —justo la que interesa leer cuando se revisa por que el modelo proponia algo
     * que ya no se vende— y esconder su nombre dejaria el historial ilegible sin
     * impedir ninguna escritura. Los articulos que no aparezcan quedan fuera del
     * mapa y el DTO los sirve con {@code catalogItemCode} y {@code catalogItemName}
     * nulos.
     *
     * <p>
     * &#9888; Antes de la correccion del adaptador no habia asimetria ninguna:
     * <b>los dos metodos se quedaban en {@code enabled}</b>, porque el unico filtro
     * que actuaba lo ponia el {@code @SQLRestriction} de una entidad de otra
     * feature —que este Hibernate si aplica tambien a la carga por id— y ese filtro
     * nunca ha mirado {@code status}. Publicar sobre un articulo retirado pasaba la
     * guarda. Ver {@code JpaAiHintCatalogItemQueryPort}.
     *
     * <p>
     * Existe ademas para que el listado no haga N+1: una pagina de veinte pistas se
     * resuelve con una consulta, no con veinte.
     */
    Map<Long, CatalogItemRef> findAllByIds(Collection<Long> catalogItemIds);
}
