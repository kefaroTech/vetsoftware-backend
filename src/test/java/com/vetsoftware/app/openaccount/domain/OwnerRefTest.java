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

@DisplayName("OwnerRef — companion VO de propietario")
class OwnerRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo")
        void conserva_cada_campo() {
            OwnerRef ref = new OwnerRef(2L, "Juan Perez", "CC123");

            assertThat(ref.id()).isEqualTo(2L);
            assertThat(ref.name()).isEqualTo("Juan Perez");
            assertThat(ref.document()).isEqualTo("CC123");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new OwnerRef(null, "Juan Perez", "CC123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un name en blanco")
        void rechaza_name_en_blanco(String name) {
            assertThatThrownBy(() -> new OwnerRef(2L, name, "CC123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un document en blanco")
        void rechaza_document_en_blanco(String document) {
            assertThatThrownBy(() -> new OwnerRef(2L, "Juan Perez", document))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner document is required");
        }

        @Test
        @DisplayName("los datos validos no lanzan")
        void los_datos_validos_no_lanzan() {
            assertThatCode(() -> new OwnerRef(2L, "Juan Perez", "CC123"))
                    .doesNotThrowAnyException();
        }
    }
}
