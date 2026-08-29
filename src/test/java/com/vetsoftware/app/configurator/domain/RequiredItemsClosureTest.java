package com.vetsoftware.app.configurator.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El cierre de requisitos, en puro. Nueve arcos {@code REQUIRES} llevaban desde
 * el changeset 309 sin que los aplicara nadie: lo que se fija aqui es que
 * aplicarlos no invente cantidades ni se cuelgue con un ciclo.
 */
@DisplayName("RequiredItemsClosure — completar el carrito con lo que necesita")
class RequiredItemsClosureTest {

    private static final Long FACTURACION = 1L;
    private static final Long CAJA = 2L;
    private static final Long ALMACENAMIENTO = 3L;
    private static final Long LABORATORIO = 4L;
    private static final Long HISTORIA = 5L;
    private static final Long SUELTO = 6L;

    @Nested
    @DisplayName("lo que anade")
    class Anade {

        @Test
        @DisplayName("anade el requisito directo que falta, con cantidad uno")
        void anade_el_requisito_directo() {
            List<SelectedItem> resultado = RequiredItemsClosure.expand(
                    List.of(new SelectedItem(FACTURACION, 1)), Map.of(FACTURACION, Set.of(CAJA)));

            assertThat(resultado).containsExactly(new SelectedItem(FACTURACION, 1),
                    new SelectedItem(CAJA, 1));
        }

        /**
         * La semilla encadena {@code EXTRA_STORAGE → LAB_IMAGING → CLINICAL_HISTORY}.
         * Anadir solo el primer salto dejaria un carrito que sigue sin funcionar.
         */
        @Test
        @DisplayName("el cierre es transitivo: arrastra la cadena entera, no un salto")
        void el_cierre_es_transitivo() {
            List<SelectedItem> resultado = RequiredItemsClosure.expand(
                    List.of(new SelectedItem(ALMACENAMIENTO, 5)),
                    Map.of(ALMACENAMIENTO, Set.of(LABORATORIO), LABORATORIO, Set.of(HISTORIA)));

            assertThat(resultado).extracting(SelectedItem::catalogItemId)
                    .containsExactly(ALMACENAMIENTO, LABORATORIO, HISTORIA);
        }

        /**
         * Un requisito es una casilla que se enciende, no una cantidad que se herede:
         * pedir cinco unidades de almacenamiento no son cinco laboratorios.
         */
        @Test
        @DisplayName("lo anadido lleva cantidad uno, no la del articulo que lo exige")
        void lo_anadido_lleva_cantidad_uno() {
            List<SelectedItem> resultado = RequiredItemsClosure.expand(
                    List.of(new SelectedItem(ALMACENAMIENTO, 5)),
                    Map.of(ALMACENAMIENTO, Set.of(LABORATORIO)));

            assertThat(resultado).filteredOn(i -> LABORATORIO.equals(i.catalogItemId()))
                    .singleElement().satisfies(i -> assertThat(i.quantity()).isEqualTo(1));
        }
    }

    @Nested
    @DisplayName("lo que no toca")
    class NoToca {

        @Test
        @DisplayName("si el requisito ya estaba, respeta su cantidad y no lo duplica")
        void respeta_la_cantidad_del_que_ya_estaba() {
            List<SelectedItem> resultado = RequiredItemsClosure.expand(
                    List.of(new SelectedItem(FACTURACION, 1), new SelectedItem(CAJA, 4)),
                    Map.of(FACTURACION, Set.of(CAJA)));

            assertThat(resultado).containsExactly(new SelectedItem(FACTURACION, 1),
                    new SelectedItem(CAJA, 4));
        }

        @Test
        @DisplayName("un articulo sin requisitos sale tal cual")
        void un_articulo_sin_requisitos_sale_tal_cual() {
            List<SelectedItem> resultado = RequiredItemsClosure.expand(
                    List.of(new SelectedItem(SUELTO, 2)), Map.of(FACTURACION, Set.of(CAJA)));

            assertThat(resultado).containsExactly(new SelectedItem(SUELTO, 2));
        }

        @Test
        @DisplayName("sin grafo de requisitos el carrito no cambia")
        void sin_grafo_el_carrito_no_cambia() {
            assertThat(RequiredItemsClosure.expand(List.of(new SelectedItem(SUELTO, 1)), Map.of()))
                    .containsExactly(new SelectedItem(SUELTO, 1));
            assertThat(RequiredItemsClosure.expand(List.of(new SelectedItem(SUELTO, 1)), null))
                    .containsExactly(new SelectedItem(SUELTO, 1));
        }

        @Test
        @DisplayName("un carrito vacio sigue vacio")
        void un_carrito_vacio_sigue_vacio() {
            assertThat(RequiredItemsClosure.expand(List.of(), Map.of(FACTURACION, Set.of(CAJA))))
                    .isEmpty();
            assertThat(RequiredItemsClosure.expand(null, Map.of())).isEmpty();
        }
    }

    /**
     * {@code DependencyGraph} impide los ciclos al guardar, pero este codigo no
     * puede darlo por hecho: un ciclo en los datos colgaria un endpoint publico,
     * que es una via de saturacion gratuita.
     */
    @Test
    @DisplayName("un ciclo entre requisitos termina en vez de colgar el endpoint")
    void un_ciclo_entre_requisitos_termina() {
        List<SelectedItem> resultado = RequiredItemsClosure.expand(
                List.of(new SelectedItem(FACTURACION, 1)),
                Map.of(FACTURACION, Set.of(CAJA), CAJA, Set.of(FACTURACION)));

        assertThat(resultado).extracting(SelectedItem::catalogItemId).containsExactly(FACTURACION,
                CAJA);
    }
}
