package com.vetsoftware.app.catalogitem.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El grafo de arcos {@code REQUIRES} del catálogo, y el detector de ciclos que
 * implementa la <strong>regla R16</strong> de
 * {@code suscripciones-reglas-codigo.md}.
 *
 * <p>
 * <strong>Qué se rompe sin esto:</strong> si A requiere B, B requiere C y C
 * requiere A, el configurador entra en bucle resolviendo dependencias y <em>no
 * se puede cotizar</em>. El ciclo directo ({@code A → A}) lo cierra
 * {@code chk_catalog_item_dependencies_not_self}; el indirecto no es expresable
 * en un {@code CHECK} de MySQL —no admite subconsultas ni columnas de otras
 * tablas— y por eso baja al código.
 *
 * <p>
 * <strong>Solo se recorren los arcos {@code REQUIRES}</strong>, y solo los
 * activos. {@code RECOMMENDS} no arrastra nada y un «ciclo» de recomendaciones
 * es inofensivo; {@code EXCLUDES} no encadena. Es el mismo criterio que usa la
 * consulta de vigilancia recursiva de la regla, para que las dos digan lo
 * mismo.
 *
 * <p>
 * <strong>Por qué no lleva tope de profundidad</strong> y la consulta SQL sí:
 * aquella recorre con una CTE recursiva que, ante un ciclo ya presente, agota
 * {@code cte_max_recursion_depth} y muere con un error en vez de dar el
 * diagnóstico — de ahí su {@code profundidad < 20}. Este recorrido es un BFS
 * con conjunto de visitados, así que termina siempre, incluso sobre un grafo
 * que ya viniera ciclado.
 *
 * <p>
 * Clase de dominio pura: cero Spring, cero JPA, cero consultas. Recibe los
 * arcos ya cargados y responde en memoria.
 */
public final class DependencyGraph {

    private final Map<Long, List<Long>> adyacencia;

    private DependencyGraph(Map<Long, List<Long>> adyacencia) {
        this.adyacencia = adyacencia;
    }

    /**
     * Construye el grafo desde los arcos {@code REQUIRES} activos. Los duplicados
     * no molestan: el BFS los descarta con el conjunto de visitados.
     */
    public static DependencyGraph ofRequires(Collection<DependencyEdge> edges) {
        Map<Long, List<Long>> adyacencia = new HashMap<>();
        if (edges != null) {
            for (DependencyEdge edge : edges) {
                adyacencia.computeIfAbsent(edge.catalogItemId(), k -> new ArrayList<>())
                        .add(edge.relatedItemId());
            }
        }
        return new DependencyGraph(adyacencia);
    }

    /**
     * El ciclo que cerraría añadir el arco {@code catalogItemId → relatedItemId}, o
     * lista vacía si no cierra ninguno.
     *
     * <p>
     * El camino devuelto arranca y termina en {@code catalogItemId} —
     * {@code [A, B, C, A]}— para que el mensaje de error le enseñe al administrador
     * del catálogo exactamente por dónde se cierra el bucle, y no un «hay un ciclo»
     * que le obliga a buscarlo a mano.
     */
    public List<Long> cycleClosedBy(Long catalogItemId, Long relatedItemId) {
        if (catalogItemId == null || relatedItemId == null)
            return List.of();
        if (catalogItemId.equals(relatedItemId))
            return List.of(catalogItemId, catalogItemId);
        List<Long> vuelta = shortestPath(relatedItemId, catalogItemId);
        if (vuelta.isEmpty())
            return List.of();
        List<Long> ciclo = new ArrayList<>();
        ciclo.add(catalogItemId);
        ciclo.addAll(vuelta);
        return List.copyOf(ciclo);
    }

    /**
     * Camino más corto de {@code from} a {@code to} siguiendo arcos
     * {@code REQUIRES}, ambos extremos incluidos; lista vacía si no hay ninguno.
     *
     * <p>
     * BFS y no DFS a propósito: el camino más corto es el que mejor se lee en el
     * mensaje de error, y con el conjunto de visitados el coste es lineal en arcos
     * sobre una tabla que la ficha proyecta en decenas de filas.
     */
    public List<Long> shortestPath(Long from, Long to) {
        if (from == null || to == null)
            return List.of();
        if (from.equals(to))
            return List.of(from);
        Map<Long, Long> anterior = new HashMap<>();
        Set<Long> visitados = new HashSet<>();
        Deque<Long> cola = new ArrayDeque<>();
        visitados.add(from);
        cola.add(from);
        while (!cola.isEmpty()) {
            Long actual = cola.poll();
            for (Long siguiente : adyacencia.getOrDefault(actual, List.of())) {
                if (!visitados.add(siguiente))
                    continue;
                anterior.put(siguiente, actual);
                if (siguiente.equals(to))
                    return reconstruir(anterior, from, to);
                cola.add(siguiente);
            }
        }
        return List.of();
    }

    private static List<Long> reconstruir(Map<Long, Long> anterior, Long from, Long to) {
        List<Long> camino = new ArrayList<>();
        Long cursor = to;
        while (cursor != null) {
            camino.add(cursor);
            if (cursor.equals(from))
                break;
            cursor = anterior.get(cursor);
        }
        Collections.reverse(camino);
        return List.copyOf(camino);
    }
}
