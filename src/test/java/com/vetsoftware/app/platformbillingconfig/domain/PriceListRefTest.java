package com.vetsoftware.app.platformbillingconfig.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PriceListRef — companion VO de la tarifa por defecto")
class PriceListRefTest {

    @Test
    @DisplayName("rechaza id nulo")
    void rechaza_id_nulo() {
        assertThatThrownBy(() -> new PriceListRef(null, "LISTA-2026-01", "Tarifa 2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price list id is required");
    }

    @Test
    @DisplayName("rechaza código en blanco")
    void rechaza_codigo_en_blanco() {
        assertThatThrownBy(() -> new PriceListRef(7L, "  ", "Tarifa 2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price list code is required");
    }

    @Test
    @DisplayName("rechaza nombre en blanco")
    void rechaza_nombre_en_blanco() {
        assertThatThrownBy(() -> new PriceListRef(7L, "LISTA-2026-01", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price list name is required");
    }
}
