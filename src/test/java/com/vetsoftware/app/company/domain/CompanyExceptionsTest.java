package com.vetsoftware.app.company.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El mensaje de estas excepciones no es cosmetico: el
 * {@code GlobalExceptionHandler} lo publica como {@code detail} del
 * ProblemDetail y el nombre de la clase se convierte en el {@code code} que
 * consume el front. Cambiarlo es cambiar el contrato de la API.
 */
@DisplayName("Excepciones de dominio de company")
class CompanyExceptionsTest {

    @Nested
    @DisplayName("CompanyNotFoundException")
    class NoEncontrada {

        @Test
        @DisplayName("nombra el id que no se encontro")
        void nombra_el_id_que_no_se_encontro() {
            assertThat(new CompanyNotFoundException(42L)).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Company not found: 42");
        }

        @Test
        @DisplayName("soporta id nulo sin romperse al construir el mensaje")
        void soporta_id_nulo() {
            assertThat(new CompanyNotFoundException(null))
                    .hasMessageContaining("Company not found");
        }
    }

    @Nested
    @DisplayName("CompanyHasActiveChildrenException")
    class ConHijosActivos {

        @ParameterizedTest
        @ValueSource(strings = {"animal", "owner", "employee", "role"})
        @DisplayName("nombra el id y el tipo de hijo que bloquea el borrado")
        void nombra_el_id_y_el_tipo_de_hijo(String tipoDeHijo) {
            assertThat(new CompanyHasActiveChildrenException(7L, tipoDeHijo))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot delete company 7")
                    .hasMessageContaining("has active " + tipoDeHijo + " children");
        }
    }
}
