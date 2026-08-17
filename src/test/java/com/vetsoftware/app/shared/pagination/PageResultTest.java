package com.vetsoftware.app.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * El contrato de paginación de la aplicación, ahora único (BE-21). Reemplaza a
 * las tres suites idénticas que vivían en {@code animal},
 * {@code electronicdocument} y {@code laboratorytest}, y cubre además lo que
 * ninguna de las treinta y cinco copias comprobaba: que los metadatos
 * sobrevivan al mapeo y que el contenido no se pueda mutar por la espalda.
 */
@DisplayName("PageResult — la pagina de la capa de aplicacion")
class PageResultTest {

    @Nested
    @DisplayName("map")
    class Map {

        @Test
        @DisplayName("transforma el contenido y conserva intactos los metadatos")
        void transforma_el_contenido_y_conserva_los_metadatos() {
            PageResult<Integer> origen = new PageResult<>(List.of(1, 2, 3), 2, 10, 57L, 6);

            PageResult<String> destino = origen.map(String::valueOf);

            assertThat(destino.content()).containsExactly("1", "2", "3");
            assertThat(destino.page()).isEqualTo(2);
            assertThat(destino.pageSize()).isEqualTo(10);
            assertThat(destino.totalElements()).isEqualTo(57L);
            assertThat(destino.totalPages()).isEqualTo(6);
        }

        @Test
        @DisplayName("respeta el orden del contenido: la pagina ya venia ordenada de la BD")
        void respeta_el_orden_del_contenido() {
            PageResult<Integer> origen = new PageResult<>(List.of(30, 10, 20), 0, 3, 3L, 1);

            assertThat(origen.map(String::valueOf).content()).containsExactly("30", "10", "20");
        }

        @Test
        @DisplayName("una pagina vacia del medio sigue reportando los totales de la consulta")
        void una_pagina_vacia_sigue_reportando_sus_totales() {
            PageResult<Integer> vacia = new PageResult<>(List.of(), 9, 10, 57L, 6);

            PageResult<String> destino = vacia.map(String::valueOf);

            assertThat(destino.content()).isEmpty();
            assertThat(destino.totalElements()).isEqualTo(57L);
            assertThat(destino.totalPages()).isEqualTo(6);
        }

        @Test
        @DisplayName("no muta el origen")
        void no_muta_el_origen() {
            PageResult<Integer> origen = new PageResult<>(List.of(1, 2), 0, 10, 2L, 1);

            origen.map(String::valueOf);

            assertThat(origen.content()).containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("contenido")
    class Contenido {

        @Test
        @DisplayName("es inmutable aunque se construya desde una lista mutable")
        void es_inmutable_aunque_se_construya_desde_una_lista_mutable() {
            List<String> mutable = new ArrayList<>(List.of("a"));

            PageResult<String> pagina = new PageResult<>(mutable, 0, 20, 1L, 1);

            assertThatThrownBy(() -> pagina.content().add("b"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("no cambia si despues se toca la lista original")
        void no_cambia_si_despues_se_toca_la_lista_original() {
            List<String> mutable = new ArrayList<>(List.of("a"));
            PageResult<String> pagina = new PageResult<>(mutable, 0, 20, 1L, 1);

            mutable.add("b");

            assertThat(pagina.content()).containsExactly("a");
        }

        @Test
        @DisplayName("null se normaliza a lista vacia: un listado nunca devuelve null")
        void null_se_normaliza_a_lista_vacia() {
            assertThat(new PageResult<>(null, 0, 20, 0L, 0).content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("of — pagina construida a mano")
    class Of {

        @ParameterizedTest(name = "{0} elementos de {1} en {1} → {2} paginas")
        @DisplayName("deriva totalPages del total y el tamano, redondeando hacia arriba")
        @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "57, 10, 6", "200, 200, 1"})
        void deriva_total_pages(long totalElements, int pageSize, int esperado) {
            PageResult<String> pagina = PageResult.of(List.of(), 0, pageSize, totalElements);

            assertThat(pagina.totalPages()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("un tamano invalido cae al tamano por defecto en lugar de dividir por cero")
        void un_tamano_invalido_cae_al_defecto() {
            PageResult<String> pagina = PageResult.of(List.of(), 0, 0, 41L);

            assertThat(pagina.pageSize()).isEqualTo(Pages.DEFAULT_SIZE);
            assertThat(pagina.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("conserva el contenido y la posicion que recibe")
        void conserva_el_contenido_y_la_posicion() {
            PageResult<String> pagina = PageResult.of(List.of("a", "b"), 3, 2, 8L);

            assertThat(pagina.content()).containsExactly("a", "b");
            assertThat(pagina.page()).isEqualTo(3);
            assertThat(pagina.totalPages()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("empty")
    class Empty {

        @Test
        @DisplayName("conserva la posicion pedida y reporta cero en todo lo demas")
        void conserva_la_posicion_pedida() {
            PageResult<String> vacia = PageResult.empty(4, 25);

            assertThat(vacia.content()).isEmpty();
            assertThat(vacia.page()).isEqualTo(4);
            assertThat(vacia.pageSize()).isEqualTo(25);
            assertThat(vacia.totalElements()).isZero();
            assertThat(vacia.totalPages()).isZero();
        }
    }
}
