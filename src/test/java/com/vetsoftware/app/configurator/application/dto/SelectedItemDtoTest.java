package com.vetsoftware.app.configurator.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Una linea de la seleccion, ya fuera del dominio.
 *
 * <p>
 * Sale por <b>rotulo</b>: el id es una llave de escritura y, secuencial en una
 * respuesta anonima, un oraculo con el que enumerar el catalogo.
 */
@DisplayName("SelectedItemDto — una linea de la seleccion")
class SelectedItemDtoTest {

    @Test
    @DisplayName("lleva el rotulo del articulo y su cantidad")
    void copia_articulo_y_cantidad() {
        SelectedItemDto dto = new SelectedItemDto("SCHEDULING", 7);

        assertThat(dto.code()).isEqualTo("SCHEDULING");
        assertThat(dto.quantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("el rotulo sustituye al id: la linea no publica ninguna llave interna")
    void el_rotulo_sustituye_al_id() {
        assertThat(SelectedItemDto.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("code", "quantity").doesNotContain("catalogItemId");
    }
}
