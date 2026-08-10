package com.vetsoftware.app.laboratorytest.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PageResult del modulo laboratorytest")
class PageResultTest {

    @Test
    @DisplayName("map transforma el contenido y conserva intacta la paginacion")
    void map_transforma_el_contenido_y_conserva_la_paginacion() {
        PageResult<Integer> origen = new PageResult<>(List.of(1, 2, 3), 2, 10, 57L, 6);

        PageResult<String> destino = origen.map(String::valueOf);

        assertThat(destino.content()).containsExactly("1", "2", "3");
        assertThat(destino.page()).isEqualTo(2);
        assertThat(destino.pageSize()).isEqualTo(10);
        assertThat(destino.totalElements()).isEqualTo(57L);
        assertThat(destino.totalPages()).isEqualTo(6);
    }

    @Test
    @DisplayName("map respeta el orden del contenido")
    void map_respeta_el_orden_del_contenido() {
        PageResult<Integer> origen = new PageResult<>(List.of(3, 1, 2), 0, 3, 3L, 1);

        assertThat(origen.map(String::valueOf).content()).containsExactly("3", "1", "2");
    }

    @Test
    @DisplayName("una pagina vacia sigue reportando los totales de la busqueda")
    void una_pagina_vacia_sigue_reportando_los_totales() {
        PageResult<Integer> vacia = new PageResult<>(List.of(), 9, 10, 57L, 6);

        PageResult<String> destino = vacia.map(String::valueOf);

        assertThat(destino.content()).isEmpty();
        assertThat(destino.totalElements()).isEqualTo(57L);
        assertThat(destino.totalPages()).isEqualTo(6);
    }

    @Test
    @DisplayName("map no muta la pagina de origen")
    void map_no_muta_la_pagina_de_origen() {
        PageResult<Integer> origen = new PageResult<>(List.of(1, 2), 0, 2, 2L, 1);

        origen.map(String::valueOf);

        assertThat(origen.content()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("encadenar dos map compone las transformaciones")
    void encadenar_dos_map_compone_las_transformaciones() {
        PageResult<Integer> origen = new PageResult<>(List.of(1, 2), 1, 2, 4L, 2);

        PageResult<Integer> destino = origen.map(String::valueOf).map(String::length);

        assertThat(destino.content()).containsExactly(1, 1);
        assertThat(destino.page()).isEqualTo(1);
    }
}
