package com.vetsoftware.app.numberingresolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CompanyRef")
class CompanyRefTest {

    @ParameterizedTest(name = "{3}")
    @MethodSource("invariantesInvalidas")
    @DisplayName("rechaza datos que violan un invariante")
    void rechaza_datos_invalidos(Long id, String name, String identifier, String mensajeEsperado) {
        assertThatThrownBy(() -> new CompanyRef(id, name, identifier))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensajeEsperado);
    }

    static Stream<Arguments> invariantesInvalidas() {
        return Stream.of(
                Arguments.of(null, "Veterinaria Central", "900123456", "company id is required"),
                Arguments.of(1L, null, "900123456", "company name is required"),
                Arguments.of(1L, "   ", "900123456", "company name is required"),
                Arguments.of(1L, "Veterinaria Central", null, "company identifier is required"),
                Arguments.of(1L, "Veterinaria Central", "   ", "company identifier is required"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("una referencia valida expone sus tres campos")
    void una_referencia_valida_expone_sus_campos() {
        CompanyRef ref = new CompanyRef(1L, "Veterinaria Central", "900123456");

        assertThat(ref.id()).isEqualTo(1L);
        assertThat(ref.name()).isEqualTo("Veterinaria Central");
        assertThat(ref.identifier()).isEqualTo("900123456");
    }
}
