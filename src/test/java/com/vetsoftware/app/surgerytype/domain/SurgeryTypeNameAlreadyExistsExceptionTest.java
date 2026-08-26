package com.vetsoftware.app.surgerytype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mensaje es medio contrato de la API: el {@code GlobalExceptionHandler} lo
 * emite tal cual en el {@code detail} del ProblemDetail que ve el formulario.
 * Si dejara de nombrar el tipo que choca, la usuaria leeria un 409 que no le
 * dice cual de sus nombres esta repetido.
 */
@DisplayName("SurgeryTypeNameAlreadyExistsException")
class SurgeryTypeNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el nombre repetido")
    void el_mensaje_incluye_el_nombre_repetido() {
        SurgeryTypeNameAlreadyExistsException ex = new SurgeryTypeNameAlreadyExistsException(
                "Castracion");

        assertThat(ex.getMessage()).contains("Castracion");
    }

    @Test
    @DisplayName("el mensaje esta en espanol y nombra el ambito, no solo la tabla")
    void el_mensaje_esta_en_espanol_y_nombra_el_ambito() {
        // Lo que sustituye al "Database constraint violation" en ingles y sin campo
        // que daba la constraint antes de #559.
        SurgeryTypeNameAlreadyExistsException ex = new SurgeryTypeNameAlreadyExistsException(
                "Castracion");

        assertThat(ex.getMessage()).contains("tipo de cirugía").contains("este ámbito");
    }
}
