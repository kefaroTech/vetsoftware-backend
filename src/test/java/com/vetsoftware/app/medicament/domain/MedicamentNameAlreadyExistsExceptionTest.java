package com.vetsoftware.app.medicament.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedicamentNameAlreadyExistsException")
class MedicamentNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje nombra el medicamento repetido y va en español")
    void el_mensaje_nombra_el_medicamento_repetido() {
        MedicamentNameAlreadyExistsException ex = new MedicamentNameAlreadyExistsException(
                "Amoxicilina");

        assertThat(ex.getMessage()).contains("Amoxicilina").contains("Ya existe un medicamento");
    }

    /**
     * El mensaje habla de ambito y no de empresa a proposito: el mismo choque
     * ocurre en el vademecum de plataforma, donde no hay empresa ninguna, y un
     * texto que dijera «en esta empresa» seria falso justo en ese caso.
     */
    @Test
    @DisplayName("el mensaje habla de ambito, no de empresa")
    void el_mensaje_habla_de_ambito() {
        MedicamentNameAlreadyExistsException ex = new MedicamentNameAlreadyExistsException("Suero");

        assertThat(ex.getMessage()).contains("ámbito").doesNotContain("empresa");
    }

    @Test
    @DisplayName("es RuntimeException, que es lo que el handler mapea a 409")
    void es_runtime_exception() {
        assertThat(new MedicamentNameAlreadyExistsException("Suero"))
                .isInstanceOf(RuntimeException.class);
    }
}
