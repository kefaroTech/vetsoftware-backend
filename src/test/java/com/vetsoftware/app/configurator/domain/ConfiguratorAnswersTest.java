package com.vetsoftware.app.configurator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Lo que respondió el prospecto, ya normalizado.
 *
 * <p>
 * El sondeo de nulos recorre el conjunto en vez de preguntarle por
 * {@code null}: los {@code Set} inmutables de la JDK lanzan
 * {@code NullPointerException} al preguntar, así que la comprobación defensiva
 * producía exactamente la NPE sin diagnóstico que existe para evitar — hasta
 * {@link ConfiguratorAnswers#empty()} moría al construirse. Esa regresión es lo
 * que fija {@code empty_no_revienta_al_construirse}.
 */
@DisplayName("ConfiguratorAnswers — las respuestas del prospecto, normalizadas")
class ConfiguratorAnswersTest {

    @Nested
    @DisplayName("normalizacion")
    class Normalizacion {

        @Test
        @DisplayName("empty no revienta al construirse")
        void empty_no_revienta_al_construirse() {
            assertThatCode(ConfiguratorAnswers::empty).doesNotThrowAnyException();
            assertThat(ConfiguratorAnswers.empty().selectedOptionIds()).isEmpty();
            assertThat(ConfiguratorAnswers.empty().numericAnswers()).isEmpty();
        }

        @Test
        @DisplayName("null en cualquiera de las dos colecciones se normaliza a vacio")
        void null_se_normaliza_a_vacio() {
            ConfiguratorAnswers respuestas = new ConfiguratorAnswers(null, null);

            assertThat(respuestas.selectedOptionIds()).isEmpty();
            assertThat(respuestas.numericAnswers()).isEmpty();
        }

        @Test
        @DisplayName("cero es una respuesta valida: significa «no quiero ninguno»")
        void cero_es_una_respuesta_valida() {
            ConfiguratorAnswers respuestas = new ConfiguratorAnswers(Set.of(), Map.of(3L, 0));

            assertThat(respuestas.numericAnswers()).containsEntry(3L, 0);
        }

        @Test
        @DisplayName("la copia es inmutable: nadie modifica las respuestas ya recibidas")
        void la_copia_es_inmutable() {
            ConfiguratorAnswers respuestas = new ConfiguratorAnswers(new HashSet<>(Set.of(11L)),
                    new HashMap<>(Map.of(3L, 2)));

            assertThatThrownBy(() -> respuestas.selectedOptionIds().add(12L))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> respuestas.numericAnswers().put(4L, 1))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("tocar despues las colecciones de origen no cambia las respuestas")
        void tocar_el_origen_despues_no_cambia_las_respuestas() {
            Set<Long> opciones = new LinkedHashSet<>(Set.of(11L));
            Map<Long, Integer> numeros = new LinkedHashMap<>(Map.of(3L, 2));
            ConfiguratorAnswers respuestas = new ConfiguratorAnswers(opciones, numeros);

            opciones.add(12L);
            numeros.put(4L, 9);

            assertThat(respuestas.selectedOptionIds()).containsExactly(11L);
            assertThat(respuestas.numericAnswers()).containsExactlyEntriesOf(Map.of(3L, 2));
        }
    }

    @Nested
    @DisplayName("lo que se rechaza con un mensaje que dice cual venia mal")
    class Validaciones {

        @Test
        @DisplayName("un null dentro de las opciones se nombra, no se convierte en NPE")
        void un_null_dentro_de_las_opciones_se_nombra() {
            Set<Long> conNull = new HashSet<>();
            conNull.add(11L);
            conNull.add(null);

            assertThatThrownBy(() -> new ConfiguratorAnswers(conNull, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("selectedOptionIds cannot contain null");
        }

        @Test
        @DisplayName("una clave null en las respuestas numericas se nombra")
        void una_clave_null_se_nombra() {
            Map<Long, Integer> conClaveNull = new HashMap<>();
            conClaveNull.put(null, 2);

            assertThatThrownBy(() -> new ConfiguratorAnswers(Set.of(), conClaveNull))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("numericAnswers cannot contain nulls");
        }

        @Test
        @DisplayName("un valor null en las respuestas numericas se nombra")
        void un_valor_null_se_nombra() {
            Map<Long, Integer> conValorNull = new HashMap<>();
            conValorNull.put(3L, null);

            assertThatThrownBy(() -> new ConfiguratorAnswers(Set.of(), conValorNull))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("numericAnswers cannot contain nulls");
        }

        @Test
        @DisplayName("un numero negativo se rechaza diciendo de que pregunta era")
        void un_numero_negativo_se_rechaza_nombrando_la_pregunta() {
            assertThatThrownBy(() -> new ConfiguratorAnswers(Set.of(), Map.of(7L, -1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("numeric answer for question 7 cannot be negative");
        }
    }
}
