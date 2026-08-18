package com.vetsoftware.app.role.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("PermissionSummaryDto — invariantes")
class PermissionSummaryDtoTest {

    @Test
    @DisplayName("conserva cada campo")
    void conserva_cada_campo() {
        var dto = new PermissionSummaryDto(1L, 2L, "Ver animales", "ANIMAL_READ");

        assertThat(dto.rolePermissionId()).isEqualTo(1L);
        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Ver animales");
        assertThat(dto.code()).isEqualTo("ANIMAL_READ");
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("rolePermissionId null",
                        (ThrowingCallable) () -> new PermissionSummaryDto(null, 2L, "Ver animales",
                                "ANIMAL_READ"),
                        "rolePermissionId is required"),
                arguments("id null",
                        (ThrowingCallable) () -> new PermissionSummaryDto(1L, null, "Ver animales",
                                "ANIMAL_READ"),
                        "permission id is required"),
                arguments("name null",
                        (ThrowingCallable) () -> new PermissionSummaryDto(1L, 2L, null,
                                "ANIMAL_READ"),
                        "permission name is required"),
                arguments("name vacio",
                        (ThrowingCallable) () -> new PermissionSummaryDto(1L, 2L, "",
                                "ANIMAL_READ"),
                        "permission name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new PermissionSummaryDto(1L, 2L, "   ",
                                "ANIMAL_READ"),
                        "permission name is required"),
                arguments("code null",
                        (ThrowingCallable) () -> new PermissionSummaryDto(1L, 2L, "Ver animales",
                                null),
                        "permission code is required"),
                arguments("code vacio",
                        (ThrowingCallable) () -> new PermissionSummaryDto(1L, 2L, "Ver animales",
                                ""),
                        "permission code is required"),
                arguments("code en blanco", (ThrowingCallable) () -> new PermissionSummaryDto(1L,
                        2L, "Ver animales", "   "), "permission code is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_compacto_rechaza(String caso, ThrowingCallable construccion,
            String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }
}
