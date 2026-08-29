package com.vetsoftware.app.configurator.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <strong>Completa el carrito con lo que sus piezas necesitan para
 * funcionar.</strong>
 *
 * <p>
 * {@code catalog_item_dependencies} declara nueve arcos {@code REQUIRES} desde
 * el changeset 309 —facturar electronicamente necesita Caja, la hospitalizacion
 * cuelga de la historia clinica, las compras necesitan Inventario— y hasta hoy
 * <strong>no los aplicaba nadie</strong>: estaban escritos y el resolvedor no
 * los miraba. Un carrito con Facturacion Electronica y sin Caja se cotizaba tal
 * cual, y el cliente compraba algo que no puede usar.
 *
 * <p>
 * <strong>Esta es la mitad amable.</strong> Aqui se <em>anade</em> lo que
 * falta, para que el prospecto vea un carrito coherente y entienda por que
 * aparece algo que no pidio. La otra mitad —rechazar un carrito incoherente—
 * vive en la cotizacion, porque una garantia que solo existe en el camino
 * amable no es una garantia: quien llame directo al endpoint de contratacion se
 * la salta.
 *
 * <p>
 * <strong>El cierre es transitivo, y hace falta que lo sea.</strong> La semilla
 * encadena {@code EXTRA_STORAGE → LAB_IMAGING → CLINICAL_HISTORY}: anadir solo
 * el primer salto dejaria un carrito que sigue sin poder funcionar. Se recorre
 * en anchura con un conjunto de visitados, asi que un ciclo entre requisitos
 * —que {@code DependencyGraph} ya impide al guardar, pero que este codigo no
 * puede dar por hecho— termina igualmente en vez de colgar el endpoint publico.
 *
 * <p>
 * <strong>Lo anadido entra con cantidad uno.</strong> Un requisito es una
 * casilla que hay que encender, no una cantidad que se herede: que alguien pida
 * tres terminales no significa que necesite tres modulos de Caja. Y si el
 * articulo ya estaba en el carrito, se respeta su cantidad: el cierre nunca
 * pisa lo que el cuestionario decidio.
 */
public final class RequiredItemsClosure {

    private RequiredItemsClosure() {
    }

    /**
     * El carrito con sus requisitos transitivos anadidos.
     *
     * @param items
     *            lo que resolvio {@link ConfiguratorResolver}
     * @param requiredBy
     *            articulo -> articulos que necesita para funcionar. Un articulo
     *            ausente del mapa no necesita nada.
     * @return los items originales, en su orden y con su cantidad, seguidos de los
     *         requisitos que faltaban, cada uno con cantidad uno
     */
    public static List<SelectedItem> expand(List<SelectedItem> items,
            Map<Long, Set<Long>> requiredBy) {
        if (items == null || items.isEmpty() || requiredBy == null || requiredBy.isEmpty()) {
            return items == null ? List.of() : List.copyOf(items);
        }

        Set<Long> enElCarrito = new LinkedHashSet<>();
        for (SelectedItem item : items) {
            enElCarrito.add(item.catalogItemId());
        }

        List<SelectedItem> resultado = new ArrayList<>(items);
        Deque<Long> porVisitar = new ArrayDeque<>(enElCarrito);
        Set<Long> visitados = new LinkedHashSet<>(enElCarrito);

        while (!porVisitar.isEmpty()) {
            Long actual = porVisitar.removeFirst();
            for (Long requisito : requiredBy.getOrDefault(actual, Set.of())) {
                if (!visitados.add(requisito)) {
                    continue;
                }
                porVisitar.addLast(requisito);
                resultado.add(new SelectedItem(requisito, 1));
            }
        }
        return List.copyOf(resultado);
    }
}
