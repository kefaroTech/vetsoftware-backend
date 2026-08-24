package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.configurator.domain.SelectedItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Una línea de la selección, ya fuera del dominio. */
@DisplayName("SelectedItemDto — una linea de la seleccion")
class SelectedItemDtoTest {

    @Test
    @DisplayName("copia articulo y cantidad tal cual salen del resolvedor")
    void copia_articulo_y_cantidad() {
        SelectedItemDto dto = SelectedItemDto.from(new SelectedItem(ITEM_POS, 7));

        assertThat(dto.catalogItemId()).isEqualTo(ITEM_POS);
        assertThat(dto.quantity()).isEqualTo(7);
    }
}
