package com.vetsoftware.app.electronicdocument.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pagina de resultados propia de la feature (el vertical slicing prohibe
 * compartirla). Lo unico con comportamiento es {@code map}: transforma el
 * contenido SIN tocar los metadatos de paginacion, que es justo donde un
 * refactor descuidado deja el total en cero.
 */
@DisplayName("PageResult — pagina de documentos electronicos")
class PageResultTest {

    @Test
    @DisplayName("map transforma el contenido y conserva los metadatos de paginacion")
    void map_transforma_el_contenido_y_conserva_los_metadatos() {
        PageResult<Long> pagina = new PageResult<>(List.of(1L, 2L, 3L), 2, 20, 47L, 3);

        PageResult<String> mapeada = pagina.map(id -> "doc-" + id);

        assertThat(mapeada.content()).containsExactly("doc-1", "doc-2", "doc-3");
        assertThat(mapeada.page()).isEqualTo(2);
        assertThat(mapeada.pageSize()).isEqualTo(20);
        assertThat(mapeada.totalElements()).isEqualTo(47L);
        assertThat(mapeada.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("map sobre una pagina vacia devuelve una pagina vacia, no falla")
    void map_sobre_una_pagina_vacia_no_falla() {
        PageResult<Long> vacia = new PageResult<>(List.of(), 0, 20, 0L, 0);

        assertThat(vacia.map(Object::toString).content()).isEmpty();
    }

    @Test
    @DisplayName("map conserva el orden del contenido original")
    void map_conserva_el_orden() {
        PageResult<Long> pagina = new PageResult<>(List.of(30L, 10L, 20L), 0, 20, 3L, 1);

        assertThat(pagina.map(id -> id).content()).containsExactly(30L, 10L, 20L);
    }

    @Test
    @DisplayName("map no muta la pagina original")
    void map_no_muta_la_pagina_original() {
        PageResult<Long> pagina = new PageResult<>(List.of(1L, 2L), 0, 20, 2L, 1);

        pagina.map(id -> "doc-" + id);

        assertThat(pagina.content()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("expone los metadatos tal como se construyeron")
    void expone_los_metadatos_tal_como_se_construyeron() {
        PageResult<String> pagina = new PageResult<>(List.of("a"), 5, 10, 51L, 6);

        assertThat(pagina.content()).containsExactly("a");
        assertThat(pagina.page()).isEqualTo(5);
        assertThat(pagina.pageSize()).isEqualTo(10);
        assertThat(pagina.totalElements()).isEqualTo(51L);
        assertThat(pagina.totalPages()).isEqualTo(6);
    }
}
