package com.vetsoftware.app.catalogitem.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La red de la <strong>regla R16</strong>: las dependencias entre artículos no
 * pueden formar ciclos indirectos.
 *
 * <p>
 * El ciclo directo lo cierra {@code chk_catalog_item_dependencies_not_self} en
 * la base. El indirecto no es expresable en un {@code CHECK} de MySQL, así que
 * esta clase es lo único que separa al configurador de un bucle infinito y de
 * un catálogo que no se puede cotizar.
 */
@DisplayName("DependencyGraph — detección de ciclos (R16)")
class DependencyGraphTest {

    private static DependencyEdge arco(long desde, long hasta) {
        return new DependencyEdge(desde, hasta);
    }

    @Nested
    @DisplayName("Ciclos que hay que rechazar")
    class Ciclos {

        @Test
        @DisplayName("detecta el ciclo indirecto A - B - C - A y devuelve la ruta completa")
        void detecta_el_ciclo_indirecto_de_tres_saltos() {
            DependencyGraph graph = DependencyGraph.ofRequires(List.of(arco(2L, 3L), arco(3L, 1L)));

            List<Long> ciclo = graph.cycleClosedBy(1L, 2L);

            assertThat(ciclo).containsExactly(1L, 2L, 3L, 1L);
        }

        @Test
        @DisplayName("detecta el ciclo indirecto más corto A - B - A")
        void detecta_el_ciclo_de_dos_saltos() {
            DependencyGraph graph = DependencyGraph.ofRequires(List.of(arco(2L, 1L)));

            assertThat(graph.cycleClosedBy(1L, 2L)).containsExactly(1L, 2L, 1L);
        }

        @Test
        @DisplayName("detecta el ciclo aunque el camino de vuelta sea largo")
        void detecta_el_ciclo_con_camino_de_vuelta_largo() {
            DependencyGraph graph = DependencyGraph
                    .ofRequires(List.of(arco(2L, 3L), arco(3L, 4L), arco(4L, 5L), arco(5L, 1L)));

            assertThat(graph.cycleClosedBy(1L, 2L)).containsExactly(1L, 2L, 3L, 4L, 5L, 1L);
        }

        @Test
        @DisplayName("el arco a sí mismo es un ciclo de longitud uno")
        void el_arco_a_si_mismo_es_un_ciclo() {
            DependencyGraph graph = DependencyGraph.ofRequires(List.of());

            assertThat(graph.cycleClosedBy(7L, 7L)).containsExactly(7L, 7L);
        }
    }

    @Nested
    @DisplayName("Grafos sanos: no se rechaza nada")
    class SinCiclo {

        @Test
        @DisplayName("un grafo vacío no cierra ningún ciclo")
        void grafo_vacio_no_cierra_ciclo() {
            assertThat(DependencyGraph.ofRequires(List.of()).cycleClosedBy(1L, 2L)).isEmpty();
        }

        @Test
        @DisplayName("una cadena lineal admite un arco más sin ciclar")
        void cadena_lineal_admite_un_arco_mas() {
            DependencyGraph graph = DependencyGraph.ofRequires(List.of(arco(1L, 2L), arco(2L, 3L)));

            assertThat(graph.cycleClosedBy(3L, 4L)).isEmpty();
        }

        @Test
        @DisplayName("un rombo no es un ciclo: dos caminos que confluyen son legítimos")
        void un_rombo_no_es_un_ciclo() {
            DependencyGraph graph = DependencyGraph
                    .ofRequires(List.of(arco(1L, 2L), arco(1L, 3L), arco(2L, 4L)));

            assertThat(graph.cycleClosedBy(3L, 4L)).isEmpty();
        }

        @Test
        @DisplayName("un componente desconectado no cierra el ciclo del otro")
        void componente_desconectado_no_cierra_ciclo() {
            DependencyGraph graph = DependencyGraph
                    .ofRequires(List.of(arco(10L, 11L), arco(11L, 12L), arco(12L, 10L)));

            assertThat(graph.cycleClosedBy(1L, 2L)).isEmpty();
        }

        @Test
        @DisplayName("null en cualquiera de los extremos no cierra ningún ciclo")
        void extremos_nulos_no_cierran_ciclo() {
            DependencyGraph graph = DependencyGraph.ofRequires(List.of(arco(1L, 2L)));

            assertThat(graph.cycleClosedBy(null, 2L)).isEmpty();
            assertThat(graph.cycleClosedBy(1L, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Robustez del recorrido")
    class Robustez {

        /**
         * La consulta de vigilancia SQL de R16 necesita un tope
         * {@code profundidad < 20} porque una CTE recursiva sobre un grafo ya ciclado
         * agota {@code cte_max_recursion_depth} y muere con un error en vez de dar el
         * diagnóstico. Este recorrido usa conjunto de visitados y termina igual, que es
         * justo lo que hay que demostrar: si el grafo llegara ciclado por cualquier
         * vía, el detector responde en vez de colgarse.
         */
        @Test
        @DisplayName("sobre un grafo que YA viene ciclado el recorrido termina y responde")
        void sobre_un_grafo_ya_ciclado_no_se_cuelga() {
            DependencyGraph graph = DependencyGraph
                    .ofRequires(List.of(arco(1L, 2L), arco(2L, 3L), arco(3L, 1L)));

            assertThat(graph.cycleClosedBy(2L, 1L)).containsExactly(2L, 1L, 2L);
            assertThat(graph.shortestPath(1L, 3L)).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("los arcos duplicados no alteran el resultado")
        void los_arcos_duplicados_no_alteran_el_resultado() {
            DependencyGraph graph = DependencyGraph
                    .ofRequires(List.of(arco(2L, 1L), arco(2L, 1L), arco(2L, 1L)));

            assertThat(graph.cycleClosedBy(1L, 2L)).containsExactly(1L, 2L, 1L);
        }

        @Test
        @DisplayName("una colección de arcos nula se trata como grafo vacío")
        void coleccion_nula_es_grafo_vacio() {
            assertThat(DependencyGraph.ofRequires(null).cycleClosedBy(1L, 2L)).isEmpty();
        }

        @Test
        @DisplayName("devuelve el camino más corto, que es el que se lee en el error")
        void devuelve_el_camino_mas_corto() {
            DependencyGraph graph = DependencyGraph.ofRequires(
                    List.of(arco(1L, 2L), arco(2L, 5L), arco(1L, 3L), arco(3L, 4L), arco(4L, 5L)));

            assertThat(graph.shortestPath(1L, 5L)).containsExactly(1L, 2L, 5L);
        }

        @Test
        @DisplayName("el camino de un nodo a sí mismo es ese nodo")
        void camino_de_un_nodo_a_si_mismo() {
            assertThat(DependencyGraph.ofRequires(List.of()).shortestPath(4L, 4L))
                    .containsExactly(4L);
        }

        @Test
        @DisplayName("no hay camino cuando el destino es inalcanzable")
        void sin_camino_cuando_el_destino_es_inalcanzable() {
            DependencyGraph graph = DependencyGraph.ofRequires(List.of(arco(1L, 2L)));

            assertThat(graph.shortestPath(2L, 1L)).isEmpty();
        }
    }
}
