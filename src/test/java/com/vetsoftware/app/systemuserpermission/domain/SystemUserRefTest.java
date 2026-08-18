package com.vetsoftware.app.systemuserpermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SystemUserRef — invariantes")
class SystemUserRefTest {

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(arguments("id nulo", null, "admin-api", "system user id is required"),
                arguments("code nulo", 5L, null, "system user code is required"),
                arguments("code en blanco", 5L, "   ", "system user code is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("rechaza")
    void rechaza(String caso, Long id, String code, String mensaje) {
        assertThatThrownBy(() -> new SystemUserRef(id, code))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
    }

    @Test
    @DisplayName("con datos validos conserva los campos")
    void con_datos_validos_conserva_los_campos() {
        SystemUserRef ref = new SystemUserRef(5L, "admin-api");

        assertThat(ref.id()).isEqualTo(5L);
        assertThat(ref.code()).isEqualTo("admin-api");
    }
}
