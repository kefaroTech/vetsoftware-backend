package com.vetsoftware.app.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * El puente con Spring Data, y el único sitio donde se acota lo que pide el
 * cliente. Lo que se prueba aquí no es aritmética: es que
 * {@code ?pageSize=100000} no vuelva a devolver la tabla entera y que
 * {@code ?page=-1} no reviente desde dentro de Spring Data, que es como estaban
 * tres de los adaptadores antes de BE-21.
 */
@DisplayName("Pages — el puente con Spring Data")
class PagesTest {

    @Nested
    @DisplayName("normalizacion del indice")
    class Indice {

        @ParameterizedTest(name = "page={0} → {1}")
        @DisplayName("nunca es negativo")
        @CsvSource({"-1, 0", "-100, 0", "0, 0", "1, 1", "7, 7"})
        void nunca_es_negativo(int pedido, int esperado) {
            assertThat(Pages.normalizePage(pedido)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("un indice negativo no llega a PageRequest, que lo rechazaria")
        void un_indice_negativo_no_llega_a_page_request() {
            assertThat(Pages.request(-3, 10).getPageNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("acotado del tamano")
    class Tamano {

        @ParameterizedTest(name = "pageSize={0} → {1}")
        @DisplayName("cae al defecto si no es valido y se topa en el maximo")
        @CsvSource({"0, 20", "-5, 20", "1, 1", "20, 20", "200, 200", "201, 200", "100000, 200"})
        void cae_al_defecto_y_se_topa(int pedido, int esperado) {
            assertThat(Pages.normalizeSize(pedido)).isEqualTo(esperado);
        }

        @Test
        @DisplayName("el tope llega hasta el PageRequest: sin el, un query param trae la tabla")
        void el_tope_llega_hasta_el_page_request() {
            assertThat(Pages.request(0, 100_000).getPageSize()).isEqualTo(Pages.MAX_SIZE);
        }
    }

    @Nested
    @DisplayName("request")
    class Request {

        @Test
        @DisplayName("sin orden explicito devuelve la pagina sin ordenar")
        void sin_orden_explicito() {
            PageRequest request = Pages.request(2, 30);

            assertThat(request.getPageNumber()).isEqualTo(2);
            assertThat(request.getPageSize()).isEqualTo(30);
            assertThat(request.getSort().isSorted()).isFalse();
        }

        @Test
        @DisplayName("conserva el orden que decide cada adaptador")
        void conserva_el_orden_del_adaptador() {
            Sort orden = Sort.by(Sort.Direction.DESC, "id");

            assertThat(Pages.request(0, 10, orden).getSort()).isEqualTo(orden);
        }

        @Test
        @DisplayName("acota tambien cuando lleva orden")
        void acota_tambien_cuando_lleva_orden() {
            PageRequest request = Pages.request(-1, 5_000, Sort.by("name"));

            assertThat(request.getPageNumber()).isZero();
            assertThat(request.getPageSize()).isEqualTo(Pages.MAX_SIZE);
        }
    }

    @Nested
    @DisplayName("result")
    class Result {

        private static final Page<Integer> PAGINA = new PageImpl<>(List.of(1, 2, 3),
                PageRequest.of(2, 3), 10L);

        @Test
        @DisplayName("mapea el contenido y toma los metadatos del Page de Spring")
        void mapea_el_contenido_y_toma_los_metadatos() {
            PageResult<String> resultado = Pages.result(PAGINA, String::valueOf);

            assertThat(resultado.content()).containsExactly("1", "2", "3");
            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.pageSize()).isEqualTo(3);
            assertThat(resultado.totalElements()).isEqualTo(10L);
            assertThat(resultado.totalPages()).isEqualTo(4);
        }

        @Test
        @DisplayName("con el contenido ya resuelto conserva los metadatos de la pagina de ids")
        void con_el_contenido_ya_resuelto_conserva_los_metadatos() {
            PageResult<String> resultado = Pages.result(PAGINA, List.of("uno", "dos", "tres"));

            assertThat(resultado.content()).containsExactly("uno", "dos", "tres");
            assertThat(resultado.page()).isEqualTo(2);
            assertThat(resultado.pageSize()).isEqualTo(3);
            assertThat(resultado.totalElements()).isEqualTo(10L);
            assertThat(resultado.totalPages()).isEqualTo(4);
        }

        @Test
        @DisplayName("una pagina vacia no pierde el total: es lo que sostiene el paginador")
        void una_pagina_vacia_no_pierde_el_total() {
            Page<Integer> vacia = new PageImpl<>(List.of(), PageRequest.of(9, 20), 57L);

            PageResult<String> resultado = Pages.result(vacia, String::valueOf);

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isEqualTo(57L);
            assertThat(resultado.totalPages()).isEqualTo(3);
        }
    }
}
