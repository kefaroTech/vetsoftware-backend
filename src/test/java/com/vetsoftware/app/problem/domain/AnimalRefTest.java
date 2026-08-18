package com.vetsoftware.app.problem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalRef — invariantes del value object")
class AnimalRefTest {

    @Nested
    @DisplayName("construccion valida")
    class ConstruccionValida {

        @Test
        @DisplayName("conserva cada campo en su posicion")
        void conserva_cada_campo_en_su_posicion() {
            AnimalRef ref = new AnimalRef(100L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(100L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("code es opcional")
        void code_es_opcional() {
            assertThatCode(() -> new AnimalRef(100L, "Firulais", null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("id nulo se rechaza")
        void id_nulo_se_rechaza() {
            assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal id is required");
        }

        @Test
        @DisplayName("nombre nulo se rechaza")
        void nombre_nulo_se_rechaza() {
            assertThatThrownBy(() -> new AnimalRef(100L, null, "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }

        @Test
        @DisplayName("nombre en blanco se rechaza")
        void nombre_en_blanco_se_rechaza() {
            assertThatThrownBy(() -> new AnimalRef(100L, "   ", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }
    }
}
