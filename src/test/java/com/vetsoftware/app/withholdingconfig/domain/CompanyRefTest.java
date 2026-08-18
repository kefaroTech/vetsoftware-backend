package com.vetsoftware.app.withholdingconfig.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CompanyRef — invariantes del companion VO")
class CompanyRefTest {

    @Nested
    @DisplayName("construccion valida")
    class ConstruccionValida {

        @Test
        @DisplayName("conserva los tres campos")
        void conserva_los_tres_campos() {
            CompanyRef ref = new CompanyRef(1L, "Veterinaria Central", "900123456-1");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Veterinaria Central");
            assertThat(ref.identifier()).isEqualTo("900123456-1");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("id nulo se rechaza")
        void id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new CompanyRef(null, "Veterinaria Central", "900123456-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest(name = "name=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("name nulo, vacio o en blanco se rechaza")
        void name_invalido_se_rechaza(String invalido) {
            assertThatThrownBy(() -> new CompanyRef(1L, invalido, "900123456-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest(name = "identifier=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("identifier nulo, vacio o en blanco se rechaza")
        void identifier_invalido_se_rechaza(String invalido) {
            assertThatThrownBy(() -> new CompanyRef(1L, "Veterinaria Central", invalido))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }
    }
}
