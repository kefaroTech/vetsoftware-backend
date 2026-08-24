package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.configurator.domain.SelectedItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lo que sale del configurador hacia la cotización. Es inmutable a propósito:
 * lo que entra aquí acaba en líneas con precio congelado y no debe poder
 * tocarse por la espalda entre el resolvedor y quien cotiza.
 */
@DisplayName("ConfiguratorSelectionDto — la seleccion que va a cotizarse")
class ConfiguratorSelectionDtoTest {

    @Test
    @DisplayName("traduce cada linea del carrito conservando articulo y cantidad")
    void traduce_cada_linea_del_carrito() {
        ConfiguratorSelectionDto seleccion = ConfiguratorSelectionDto
                .from(List.of(new SelectedItem(ITEM_POS, 1), new SelectedItem(ITEM_CAJA, 4)));

        assertThat(seleccion.items()).containsExactly(new SelectedItemDto(ITEM_POS, 1),
                new SelectedItemDto(ITEM_CAJA, 4));
    }

    @Test
    @DisplayName("una seleccion vacia sigue siendo una seleccion, no un null")
    void una_seleccion_vacia_sigue_siendo_una_seleccion() {
        assertThat(ConfiguratorSelectionDto.from(List.of()).items()).isEmpty();
    }

    @Test
    @DisplayName("null se normaliza a lista vacia")
    void null_se_normaliza_a_lista_vacia() {
        assertThat(new ConfiguratorSelectionDto(null).items()).isEmpty();
    }

    @Test
    @DisplayName("el contenido es inmutable aunque se construya desde una lista mutable")
    void el_contenido_es_inmutable() {
        List<SelectedItemDto> mutable = new ArrayList<>(List.of(new SelectedItemDto(ITEM_POS, 1)));
        ConfiguratorSelectionDto seleccion = new ConfiguratorSelectionDto(mutable);

        assertThatThrownBy(() -> seleccion.items().add(new SelectedItemDto(ITEM_CAJA, 2)))
                .isInstanceOf(UnsupportedOperationException.class);

        mutable.add(new SelectedItemDto(ITEM_CAJA, 2));
        assertThat(seleccion.items()).hasSize(1);
    }
}
