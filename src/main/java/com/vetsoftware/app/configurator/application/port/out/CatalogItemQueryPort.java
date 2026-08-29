package com.vetsoftware.app.configurator.application.port.out;

import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import java.util.Collection;
import java.util.List;

/**
 * Traduce los ids que llevan los efectos a los <strong>rotulos</strong> con los
 * que habla el resto del mundo publico.
 *
 * <p>
 * <strong>Por que un {@code QueryPort} al lado del
 * {@link CatalogItemValidationPort} que ya habia.</strong> Aquel responde «esta
 * FK apunta a algo» y lo usa el guardado de un efecto; este trae un campo del
 * articulo, que es justo lo que aquel documentaba no necesitar. Ha dejado de
 * ser cierto: la respuesta publica ya no puede llevar el id
 * ({@link CatalogItemRef}), asi que el rotulo hay que ir a buscarlo. Se deja el
 * de validacion en su sitio porque sigue teniendo su caso de uso —el alta de un
 * efecto no necesita el codigo— y fundirlos obligaria al camino de escritura a
 * cargar datos que no mira.
 *
 * <p>
 * {@code catalog_items} es catalogo global de plataforma y no tiene
 * {@code company_id}: no hay empresa que acotar y ninguna de las reglas de
 * tenant aplica. Solo lee.
 */
public interface CatalogItemQueryPort {

    /**
     * Los articulos {@code ACTIVE} y habilitados de entre los ids pedidos.
     *
     * <p>
     * <strong>Puede devolver menos de los que se piden, y es deliberado.</strong>
     * Un efecto puede apuntar a un articulo que se retiro de la venta despues de
     * sembrarlo; ese articulo no tiene rotulo que publicar y tampoco deberia
     * aparecer en un carrito. Quien llama lo descarta en vez de inventarle un
     * nombre.
     */
    List<CatalogItemRef> findActiveByIds(Collection<Long> catalogItemIds);
}
