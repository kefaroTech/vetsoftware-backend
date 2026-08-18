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

@DisplayName("BranchRef — companion VO de sede")
class BranchRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo")
        void conserva_cada_campo() {
            BranchRef ref = new BranchRef(1L, "Principal", "PRINCIPAL");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Principal");
            assertThat(ref.code()).isEqualTo("PRINCIPAL");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new BranchRef(null, "Principal", "PRINCIPAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un name en blanco")
        void rechaza_name_en_blanco(String name) {
            assertThatThrownBy(() -> new BranchRef(1L, name, "PRINCIPAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un code en blanco")
        void rechaza_code_en_blanco(String code) {
            assertThatThrownBy(() -> new BranchRef(1L, "Principal", code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch code is required");
        }

        @Test
        @DisplayName("los datos validos no lanzan")
        void los_datos_validos_no_lanzan() {
            assertThatCode(() -> new BranchRef(1L, "Principal", "PRINCIPAL"))
                    .doesNotThrowAnyException();
        }
    }
}
