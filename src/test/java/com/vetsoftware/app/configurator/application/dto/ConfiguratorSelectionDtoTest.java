package com.vetsoftware.app.configurator.application.dto;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.configurator.domain.CatalogItemRef;
import com.vetsoftware.app.configurator.domain.SelectedItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lo que sale del configurador hacia la cotización. Es inmutable a propósito:
 * lo que entra aquí acaba en líneas con precio congelado y no debe poder
 * tocarse por la espalda entre el resolvedor y quien cotiza.
 *
 * <p>
 * Y sale <b>por rótulo</b>: aquí es donde el id interno muere. Ver
 * {@link SelectedItemDto}.
 */
@DisplayName("ConfiguratorSelectionDto — la seleccion que va a cotizarse")
class ConfiguratorSelectionDtoTest {

    private static final String COD_POS = "SCHEDULING";
    private static final String COD_CAJA = "EXTRA_TERMINAL";

    private static CatalogItemRef ref(Long id, String code) {
        return new CatalogItemRef(id, code, null, false);
    }

    @Test
    @DisplayName("traduce cada linea del carrito a su rotulo conservando la cantidad")
    void traduce_cada_linea_del_carrito() {
        ConfiguratorSelectionDto seleccion = ConfiguratorSelectionDto.from(
                List.of(new SelectedItem(ITEM_POS, 1), new SelectedItem(ITEM_CAJA, 4)),
                List.of(ref(ITEM_POS, COD_POS), ref(ITEM_CAJA, COD_CAJA)));

        assertThat(seleccion.items()).containsExactly(new SelectedItemDto(COD_POS, 1),
                new SelectedItemDto(COD_CAJA, 4));
    }

    /**
     * Un efecto puede apuntar a un articulo retirado de la venta. Sin rotulo no hay
     * linea que publicar: la contratacion la rechazaria y el front no sabria
     * pintarla.
     */
    @Test
    @DisplayName("un articulo sin rotulo se descarta, no sale con el hueco vacio")
    void un_articulo_sin_rotulo_se_descarta() {
        ConfiguratorSelectionDto seleccion = ConfiguratorSelectionDto.from(
                List.of(new SelectedItem(ITEM_POS, 1), new SelectedItem(ITEM_CAJA, 4)),
                List.of(ref(ITEM_POS, COD_POS)));

        assertThat(seleccion.items()).containsExactly(new SelectedItemDto(COD_POS, 1));
    }

    @Test
    @DisplayName("sin ninguna traduccion la seleccion sale vacia, nunca con ids crudos")
    void sin_traduccion_la_seleccion_sale_vacia() {
        assertThat(ConfiguratorSelectionDto.from(List.of(new SelectedItem(ITEM_POS, 1)), List.of())
                .items()).isEmpty();
        assertThat(
                ConfiguratorSelectionDto.from(List.of(new SelectedItem(ITEM_POS, 1)), null).items())
                .isEmpty();
    }

    @Test
    @DisplayName("una seleccion vacia sigue siendo una seleccion, no un null")
    void una_seleccion_vacia_sigue_siendo_una_seleccion() {
        assertThat(ConfiguratorSelectionDto.from(List.of(), List.of()).items()).isEmpty();
    }

    @Test
    @DisplayName("null se normaliza a lista vacia")
    void null_se_normaliza_a_lista_vacia() {
        assertThat(new ConfiguratorSelectionDto(null).items()).isEmpty();
    }

    @Test
    @DisplayName("el contenido es inmutable aunque se construya desde una lista mutable")
    void el_contenido_es_inmutable() {
        List<SelectedItemDto> mutable = new ArrayList<>(List.of(new SelectedItemDto(COD_POS, 1)));
        ConfiguratorSelectionDto seleccion = new ConfiguratorSelectionDto(mutable);

        assertThatThrownBy(() -> seleccion.items().add(new SelectedItemDto(COD_CAJA, 2)))
                .isInstanceOf(UnsupportedOperationException.class);

        mutable.add(new SelectedItemDto(COD_CAJA, 2));
        assertThat(seleccion.items()).hasSize(1);
    }
}
