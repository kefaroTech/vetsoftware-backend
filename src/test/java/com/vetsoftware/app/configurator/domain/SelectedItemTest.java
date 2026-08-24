package com.vetsoftware.app.configurator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Una línea del carrito. La invariante de cantidad positiva es la que impide
 * que una cotización impresa salga con una línea de cero unidades, que es una
 * pregunta del cliente esperando a pasar.
 */
@DisplayName("SelectedItem — una linea del carrito")
class SelectedItemTest {

    @Test
    @DisplayName("sin articulo de catalogo se rechaza")
    void sin_articulo_se_rechaza() {
        assertThatThrownBy(() -> new SelectedItem(null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogItemId is required");
    }

    @ParameterizedTest(name = "cantidad = {0}")
    @DisplayName("una cantidad de cero o negativa se rechaza: no hay linea de cero unidades")
    @ValueSource(ints = {0, -1, -100})
    void una_cantidad_no_positiva_se_rechaza(int cantidad) {
        assertThatThrownBy(() -> new SelectedItem(100L, cantidad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be greater than 0");
    }

    @Test
    @DisplayName("conserva articulo y cantidad tal como llegan")
    void conserva_articulo_y_cantidad() {
        SelectedItem linea = new SelectedItem(100L, 3);

        assertThat(linea.catalogItemId()).isEqualTo(100L);
        assertThat(linea.quantity()).isEqualTo(3);
    }
}
