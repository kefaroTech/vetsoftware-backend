package com.vetsoftware.app.branch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VO de {@code branch} hacia {@code company} (cross-feature
 * reference). Es un VO propio de esta feature — deliberadamente homónimo del
 * {@code CompanyRef} de otras features, cada una con su propio contrato.
 */
@DisplayName("CompanyRef")
class CompanyRefTest {

    @Nested
    @DisplayName("construcción válida")
    class Creacion {

        @Test
        @DisplayName("expone id, nombre e identificador tal cual se construyó")
        void expone_los_campos_tal_cual_se_construyo() {
            CompanyRef ref = new CompanyRef(9L, "Vet SAS", "900123456");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Vet SAS");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }
    }

    @Nested
    @DisplayName("invariantes")
    class Validaciones {

        @Test
        @DisplayName("el id es obligatorio")
        void el_id_es_obligatorio() {
            assertThatThrownBy(() -> new CompanyRef(null, "Vet SAS", "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest(name = "nombre inválido: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("el nombre es obligatorio: nulo, vacío o en blanco")
        void el_nombre_es_obligatorio(String nombreInvalido) {
            assertThatThrownBy(() -> new CompanyRef(9L, nombreInvalido, "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest(name = "identificador inválido: [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("el identificador es obligatorio: nulo, vacío o en blanco")
        void el_identificador_es_obligatorio(String identificadorInvalido) {
            assertThatThrownBy(() -> new CompanyRef(9L, "Vet SAS", identificadorInvalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }
}
