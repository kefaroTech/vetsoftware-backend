package com.vetsoftware.app.rolepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("PermissionRef — invariantes del value object")
class PermissionRefTest {

    @Test
    @DisplayName("acepta un permiso con id, nombre y codigo")
    void acepta_un_permiso_completo() {
        PermissionRef ref = new PermissionRef(7L, "Ver animales", "ANIMAL_READ");

        assertThat(ref.id()).isEqualTo(7L);
        assertThat(ref.name()).isEqualTo("Ver animales");
        assertThat(ref.code()).isEqualTo("ANIMAL_READ");
    }

    static Stream<Arguments> combinacionesInvalidas() {
        return Stream.of(
                Arguments.of(null, "Ver animales", "ANIMAL_READ", "permission id is required"),
                Arguments.of(7L, null, "ANIMAL_READ", "permission name is required"),
                Arguments.of(7L, "", "ANIMAL_READ", "permission name is required"),
                Arguments.of(7L, "  ", "ANIMAL_READ", "permission name is required"),
                Arguments.of(7L, "Ver animales", null, "permission code is required"),
                Arguments.of(7L, "Ver animales", "", "permission code is required"),
                Arguments.of(7L, "Ver animales", "  ", "permission code is required"));
    }

    @ParameterizedTest(name = "id={0} name={1} code={2} → {3}")
    @MethodSource("combinacionesInvalidas")
    @DisplayName("rechaza id nulo y nombre o codigo en blanco")
    void rechaza_combinaciones_invalidas(Long id, String name, String code, String mensaje) {
        assertThatThrownBy(() -> new PermissionRef(id, name, code))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
    }

    @Test
    @DisplayName("dos referencias con los mismos datos son iguales")
    void dos_referencias_iguales() {
        assertThat(new PermissionRef(7L, "Ver animales", "ANIMAL_READ"))
                .isEqualTo(new PermissionRef(7L, "Ver animales", "ANIMAL_READ"))
                .hasSameHashCodeAs(new PermissionRef(7L, "Ver animales", "ANIMAL_READ"));
    }
}
