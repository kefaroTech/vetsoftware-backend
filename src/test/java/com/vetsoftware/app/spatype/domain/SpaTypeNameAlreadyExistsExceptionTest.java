package com.vetsoftware.app.spatype.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mensaje es contrato de cara al usuario: lo pinta el front tal cual como
 * {@code detail} del ProblemDetail. Lo que se fija aqui es que nombre el campo
 * repetido — antes de #559 el choque salia como
 * {@code "Database constraint violation"}, en ingles y sin decir cual era el
 * campo.
 */
@DisplayName("SpaTypeNameAlreadyExistsException")
class SpaTypeNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el nombre repetido")
    void el_mensaje_incluye_el_nombre_repetido() {
        SpaTypeNameAlreadyExistsException ex = new SpaTypeNameAlreadyExistsException(
                "Baño medicado");

        assertThat(ex.getMessage()).contains("Baño medicado");
    }

    @Test
    @DisplayName("el mensaje esta en castellano y dice que el choque es con un tipo ACTIVO")
    void el_mensaje_esta_en_castellano_y_habla_de_un_tipo_activo() {
        SpaTypeNameAlreadyExistsException ex = new SpaTypeNameAlreadyExistsException(
                "Baño medicado");

        assertThat(ex.getMessage()).contains("Ya existe un tipo de spa activo");
    }

    @Test
    @DisplayName("es una RuntimeException: no obliga a declararla en los puertos")
    void es_una_runtime_exception() {
        assertThat(new SpaTypeNameAlreadyExistsException("X")).isInstanceOf(RuntimeException.class);
    }
}
