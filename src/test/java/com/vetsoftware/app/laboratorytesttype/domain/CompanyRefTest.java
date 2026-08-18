package com.vetsoftware.app.laboratorytesttype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CompanyRef")
class CompanyRefTest {

    @Test
    @DisplayName("construye con id, nombre e identificador validos")
    void construye_con_los_tres_campos_validos() {
        CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

        assertThat(ref.id()).isEqualTo(9L);
        assertThat(ref.name()).isEqualTo("Clinica Norte");
        assertThat(ref.identifier()).isEqualTo("NIT-900");
    }

    static Stream<Arguments> datosInvalidos() {
        return Stream.of(Arguments.of(null, "Clinica Norte", "NIT-900", "company id is required"),
                Arguments.of(9L, null, "NIT-900", "company name is required"),
                Arguments.of(9L, "  ", "NIT-900", "company name is required"),
                Arguments.of(9L, "Clinica Norte", null, "company identifier is required"),
                Arguments.of(9L, "Clinica Norte", "  ", "company identifier is required"));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("datosInvalidos")
    @DisplayName("cada invariante rechaza su combinacion invalida")
    void cada_invariante_rechaza_su_combinacion_invalida(Long id, String name, String identifier,
            String mensajeEsperado) {
        assertThatThrownBy(() -> new CompanyRef(id, name, identifier))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensajeEsperado);
    }
}
