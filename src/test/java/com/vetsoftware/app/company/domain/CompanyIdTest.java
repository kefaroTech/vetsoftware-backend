package com.vetsoftware.app.company.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyId — identificador tipado de la empresa")
class CompanyIdTest {

    @Test
    @DisplayName("of envuelve el valor recibido sin transformarlo")
    void of_envuelve_el_valor_recibido() {
        assertThat(CompanyId.of(9L).value()).isEqualTo(9L);
    }

    @Test
    @DisplayName("dos identificadores con el mismo valor son iguales")
    void dos_identificadores_con_el_mismo_valor_son_iguales() {
        assertThat(CompanyId.of(9L)).isEqualTo(new CompanyId(9L)).isNotEqualTo(CompanyId.of(10L));
    }

    @Test
    @DisplayName("no valida el valor: admite null porque el VO no lleva invariante")
    void admite_valor_nulo() {
        // Documenta el estado actual del tipo: no hay guard en el canonico. Si
        // algun dia se anade, este test es el que avisa de que el contrato cambio.
        assertThat(CompanyId.of(null).value()).isNull();
    }
}
