package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * El mecanismo que le faltaba a R1 del anexo B —"el texto libre no sale por
 * ninguna senal"—, que hasta hoy era una regla escrita y nada mas.
 *
 * <p>
 * <b>Este test es la regla.</b> Cambiar {@link ProspectText#toString()} por
 * {@code return text} abriria de par en par cada log, cada MDC y cada atributo
 * de span del backend a la vez, y en el diff se leeria como una simplificacion
 * razonable. Aqui se cae el build.
 */
@DisplayName("ProspectText — el texto del prospecto no se puede imprimir")
class ProspectTextTest {

    private static final String CONFIDENCIAL = "Somos la veterinaria de Laura en Chapinero,"
            + " facturamos harto y atendemos consultas y vacunas";

    @Test
    @DisplayName("toString no contiene ni un fragmento del texto")
    void to_string_no_filtra() {
        ProspectText texto = ProspectText.of(CONFIDENCIAL);

        assertThat(texto.toString()).doesNotContain("Laura").doesNotContain("Chapinero")
                .doesNotContain("veterinaria").contains("chars");
    }

    @Test
    @DisplayName("la concatenacion de un log tampoco lo filtra: es el mismo toString")
    void la_concatenacion_no_filtra() {
        // Exactamente lo que escribe quien esta depurando: log.info("... {}", texto).
        String comoLoVeriaUnLog = "propuesta=" + ProspectText.of(CONFIDENCIAL);

        assertThat(comoLoVeriaUnLog).doesNotContain("Laura").doesNotContain("Chapinero");
    }

    @Test
    @DisplayName("y el MDC, que es por donde viajan las etiquetas a Loki")
    void el_mdc_tampoco() {
        try {
            MDC.put("prospect.text", String.valueOf(ProspectText.of(CONFIDENCIAL)));

            assertThat(MDC.get("prospect.text")).doesNotContain("Laura")
                    .doesNotContain("Chapinero");
        } finally {
            MDC.remove("prospect.text");
        }
    }

    @Test
    @DisplayName("y el atributo de un span, que es la ruta que nadie recuerda")
    void el_span_tampoco() {
        // Esta era la mitad que faltaba. Un span NO pasa por RedactingAppender -es
        // un appender de Logback y las trazas salen por OTLP directo-, asi que un
        // atributo con el texto del prospecto viajaria en claro a Estados Unidos al
        // 100 % de muestreo sin que ninguna otra prueba se enterase. Lo que lo
        // impide es el mismo toString(), porque un KeyValue exige una cadena.
        List<Observation.Context> capturados = new ArrayList<>();
        ObservationRegistry registro = ObservationRegistry.create();
        registro.observationConfig()
                .observationHandler(new ObservationHandler<Observation.Context>() {

                    @Override
                    public void onStop(Observation.Context context) {
                        capturados.add(context);
                    }

                    @Override
                    public boolean supportsContext(Observation.Context context) {
                        return true;
                    }
                });

        Observation.createNotStarted("aiproposal.generate", registro).highCardinalityKeyValue(
                "prospect.text", String.valueOf(ProspectText.of(CONFIDENCIAL))).observe(() -> {
                });

        assertThat(capturados).singleElement()
                .satisfies(contexto -> assertThat(contexto.getAllKeyValues())
                        .noneMatch(kv -> kv.getValue().contains("Laura")
                                || kv.getValue().contains("Chapinero")));
    }

    @Test
    @DisplayName("la longitud si sale: es la unica medida que el anexo B autoriza")
    void la_longitud_si_sale() {
        assertThat(ProspectText.of(CONFIDENCIAL).length()).isEqualTo(CONFIDENCIAL.length());
    }

    @Test
    @DisplayName("el texto de verdad exige nombrar el metodo que lo destapa")
    void el_texto_se_destapa_a_proposito() {
        assertThat(ProspectText.of(CONFIDENCIAL).revealForModelCall()).isEqualTo(CONFIDENCIAL);
    }

    @Test
    @DisplayName("no hay accesor con forma de propiedad que Jackson pueda encontrar")
    void sin_getters_serializables() {
        assertThat(ProspectText.class.getMethods()).noneMatch(
                metodo -> metodo.getName().startsWith("get") && metodo.getParameterCount() == 0
                        && metodo.getReturnType() == String.class);
    }

    @Test
    @DisplayName("dos textos iguales son iguales: hace falta para deduplicar turnos")
    void igualdad_por_contenido() {
        assertThat(ProspectText.of(CONFIDENCIAL)).isEqualTo(ProspectText.of(CONFIDENCIAL))
                .hasSameHashCodeAs(ProspectText.of(CONFIDENCIAL));
        assertThat(ProspectText.of(CONFIDENCIAL)).isNotEqualTo(ProspectText.of("otra cosa"));
    }

    @Test
    @DisplayName("vacio y mas de mil caracteres no se construyen")
    void invariantes() {
        assertThatThrownBy(() -> ProspectText.of("  ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> ProspectText.of("x".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1000");
    }
}
