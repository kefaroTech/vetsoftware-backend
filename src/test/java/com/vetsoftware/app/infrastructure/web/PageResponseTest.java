package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La traducción de la página de la aplicación a la de la web. Antes de BE-21
 * era un bloque de cuatro líneas repetido en los 41 endpoints paginados; lo que
 * se comprueba aquí es lo único que ese bloque podía equivocar en silencio: los
 * totales son los de la consulta, no los del contenido de la página.
 */
@DisplayName("PageResponse — la pagina tal como sale por HTTP")
class PageResponseTest {

    @Test
    @DisplayName("mapea el contenido y arrastra los metadatos sin tocarlos")
    void mapea_el_contenido_y_arrastra_los_metadatos() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2, 3), 2, 20, 57L, 3);

        PageResponse<String> response = PageResponse.from(result, i -> "n" + i);

        assertThat(response.content()).containsExactly("n1", "n2", "n3");
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(57L);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("el total no se recalcula sobre el contenido de la pagina")
    void el_total_no_se_recalcula_sobre_el_contenido() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2, 3), 0, 3, 900L, 300);

        PageResponse<String> response = PageResponse.from(result, String::valueOf);

        assertThat(response.content()).hasSize(3);
        assertThat(response.totalElements()).isEqualTo(900L);
    }

    @Test
    @DisplayName("sin mapeo cuando el caso de uso ya devuelve el tipo de la web")
    void sin_mapeo_cuando_el_tipo_ya_es_el_de_la_web() {
        PageResult<String> result = new PageResult<>(List.of("a", "b"), 1, 2, 4L, 2);

        PageResponse<String> response = PageResponse.from(result);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("una pagina vacia conserva los totales de la consulta")
    void una_pagina_vacia_conserva_los_totales() {
        PageResponse<String> response = PageResponse
                .from(new PageResult<Integer>(List.of(), 9, 20, 57L, 3), String::valueOf);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(57L);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
