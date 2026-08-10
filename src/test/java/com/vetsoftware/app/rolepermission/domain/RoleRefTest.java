package com.vetsoftware.app.rolepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("RoleRef — invariantes del value object")
class RoleRefTest {

    @Test
    @DisplayName("acepta un rol con id, nombre y codigo")
    void acepta_un_rol_completo() {
        RoleRef ref = new RoleRef(3L, "Veterinario", "VET");

        assertThat(ref.id()).isEqualTo(3L);
        assertThat(ref.name()).isEqualTo("Veterinario");
        assertThat(ref.code()).isEqualTo("VET");
    }

    static Stream<Arguments> combinacionesInvalidas() {
        return Stream.of(Arguments.of(null, "Veterinario", "VET", "role id is required"),
                Arguments.of(3L, null, "VET", "role name is required"),
                Arguments.of(3L, "", "VET", "role name is required"),
                Arguments.of(3L, "   ", "VET", "role name is required"),
                Arguments.of(3L, "Veterinario", null, "role code is required"),
                Arguments.of(3L, "Veterinario", "", "role code is required"),
                Arguments.of(3L, "Veterinario", "   ", "role code is required"));
    }

    @ParameterizedTest(name = "id={0} name={1} code={2} → {3}")
    @MethodSource("combinacionesInvalidas")
    @DisplayName("rechaza id nulo y nombre o codigo en blanco")
    void rechaza_combinaciones_invalidas(Long id, String name, String code, String mensaje) {
        assertThatThrownBy(() -> new RoleRef(id, name, code))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
    }

    @Test
    @DisplayName("dos referencias con los mismos datos son iguales")
    void dos_referencias_iguales() {
        assertThat(new RoleRef(3L, "Veterinario", "VET"))
                .isEqualTo(new RoleRef(3L, "Veterinario", "VET"))
                .hasSameHashCodeAs(new RoleRef(3L, "Veterinario", "VET"));
    }

    @Test
    @DisplayName("el nombre con espacios alrededor es valido: no se normaliza aqui")
    void el_nombre_con_espacios_alrededor_es_valido() {
        assertThatCode(() -> new RoleRef(3L, " Veterinario ", "VET")).doesNotThrowAnyException();
    }
}
