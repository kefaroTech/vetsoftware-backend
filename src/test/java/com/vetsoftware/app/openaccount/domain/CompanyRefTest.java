package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CompanyRef — companion VO de empresa")
class CompanyRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo")
        void conserva_cada_campo() {
            CompanyRef ref = new CompanyRef(9L, "Vet SAS", "900123456");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Vet SAS");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new CompanyRef(null, "Vet SAS", "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un name en blanco")
        void rechaza_name_en_blanco(String name) {
            assertThatThrownBy(() -> new CompanyRef(9L, name, "900123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un identifier en blanco")
        void rechaza_identifier_en_blanco(String identifier) {
            assertThatThrownBy(() -> new CompanyRef(9L, "Vet SAS", identifier))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }

        @Test
        @DisplayName("los datos validos no lanzan")
        void los_datos_validos_no_lanzan() {
            assertThatCode(() -> new CompanyRef(9L, "Vet SAS", "900123456"))
                    .doesNotThrowAnyException();
        }
    }
}
