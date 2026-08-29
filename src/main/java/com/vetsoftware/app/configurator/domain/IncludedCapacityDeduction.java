package com.vetsoftware.app.configurator.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <strong>Resta del carrito lo que el contrato ya trae puesto.</strong>
 *
 * <p>
 * Es la segunda de las «DOS REGLAS DE CODIGO QUE NO SON DATO» que declara el
 * changeset {@code 312_seed_configurator}. La primera —el orden de aplicacion
 * por {@code priority}— si se escribio, y vive en {@link ConfiguratorResolver}.
 * Esta se quedo sin escribir, y esa asimetria es la que la delata: no fue una
 * decision, fue un olvido.
 *
 * <p>
 * <strong>Lo que costaba no tenerla.</strong> El cuestionario pregunta «cuantas
 * personas sois» y traduce la respuesta a unidades de {@code EXTRA_USER} con la
 * cantidad <em>en crudo</em>. Ana, que trabaja sola y tiene una caja, salia con
 * una unidad de usuario extra y una de terminal extra —12.000 + 18.000 al mes—
 * por dos cosas que su contrato ya incluye. No fallaba nada: la cotizacion
 * salia mal y firmada.
 *
 * <p>
 * <strong>La regla, con la aritmetica que la ancla.</strong>
 * {@code max(0, respuesta - techo)}. Con la semilla 310 el techo del eje
 * {@code USER} es 2 —{@code CAPACITY_USER} trae {@code included_quantity = 1} y
 * el contrato inicial lo concede con {@code min_quantity = 1}—, asi que quince
 * personas son <strong>trece</strong> unidades facturables: 8 x 12.000 + 5 x
 * 9.000 = 141.000, que es exactamente el ancla de D-66. Una persona son cero
 * unidades, y la linea desaparece del carrito en vez de quedarse a cero: «no
 * quiero usuarios extra» y «quiero cero usuarios extra» son la misma respuesta,
 * y una linea de cero en una oferta impresa es una pregunta del cliente
 * esperando a pasar — el mismo criterio que ya aplica
 * {@code ConfiguratorResolver} al responder cero.
 *
 * <p>
 * <strong>Solo se resta a la unidad facturable.</strong> El techo lo aporta el
 * articulo {@code is_core} del mismo eje; restarselo a el lo dejaria siempre en
 * cero. Y solo a los contadores: a un modulo, que es una casilla encendida, no
 * hay techo que restarle.
 *
 * <p>
 * Puro: sin Spring, sin repositorios, sin reloj. Recibe el techo ya resuelto.
 */
public final class IncludedCapacityDeduction {

    private IncludedCapacityDeduction() {
    }

    /**
     * El carrito con las unidades ya incluidas descontadas.
     *
     * @param items
     *            lo que resolvio {@link ConfiguratorResolver}
     * @param refs
     *            los articulos de esos items, con su eje y su marca de nucleo
     * @param ceilingsByAxis
     *            eje -> unidades que el contrato ya concede. Un eje ausente es «no
     *            trae nada incluido»: no se resta nada.
     * @return los items con cantidad mayor que cero, en el mismo orden
     */
    public static List<SelectedItem> apply(List<SelectedItem> items, List<CatalogItemRef> refs,
            Map<String, Integer> ceilingsByAxis) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<Long, CatalogItemRef> porId = indexById(refs);
        Map<String, Integer> techos = ceilingsByAxis == null ? Map.of() : ceilingsByAxis;

        List<SelectedItem> resultado = new ArrayList<>();
        for (SelectedItem item : items) {
            CatalogItemRef ref = porId.get(item.catalogItemId());
            int cantidad = ref == null || !ref.esUnidadFacturable()
                    ? item.quantity()
                    : item.quantity() - techos.getOrDefault(ref.capacityUnit(), 0);
            if (cantidad > 0) {
                resultado.add(new SelectedItem(item.catalogItemId(), cantidad));
            }
        }
        return List.copyOf(resultado);
    }

    private static Map<Long, CatalogItemRef> indexById(List<CatalogItemRef> refs) {
        if (refs == null) {
            return Map.of();
        }
        Map<Long, CatalogItemRef> porId = new LinkedHashMap<>();
        for (CatalogItemRef ref : refs) {
            porId.put(ref.id(), ref);
        }
        return porId;
    }
}
