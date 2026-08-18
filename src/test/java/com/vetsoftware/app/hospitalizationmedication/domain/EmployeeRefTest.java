package com.vetsoftware.app.hospitalizationmedication.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("EmployeeRef — invariantes del value object")
class EmployeeRefTest {

    @Test
    @DisplayName("el constructor compacto conserva los tres campos")
    void el_constructor_compacto_conserva_los_tres_campos() {
        EmployeeRef ref = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");

        assertThat(ref.id()).isEqualTo(4L);
        assertThat(ref.employeeCode()).isEqualTo("EMP-001");
        assertThat(ref.name()).isEqualTo("Ana Ruiz");
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id null",
                        (ThrowingCallable) () -> new EmployeeRef(null, "EMP-001", "Ana Ruiz"),
                        "employee id is required"),
                arguments("employeeCode null",
                        (ThrowingCallable) () -> new EmployeeRef(4L, null, "Ana Ruiz"),
                        "employee code is required"),
                arguments("employeeCode en blanco",
                        (ThrowingCallable) () -> new EmployeeRef(4L, "   ", "Ana Ruiz"),
                        "employee code is required"),
                arguments("name null",
                        (ThrowingCallable) () -> new EmployeeRef(4L, "EMP-001", null),
                        "employee name is required"),
                arguments("name en blanco",
                        (ThrowingCallable) () -> new EmployeeRef(4L, "EMP-001", "   "),
                        "employee name is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("el constructor compacto rechaza")
    void el_constructor_compacto_rechaza(String caso, ThrowingCallable construccion,
            String mensaje) {
        assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }

    @Test
    @DisplayName("dos refs con los mismos valores son iguales")
    void dos_refs_con_los_mismos_valores_son_iguales() {
        assertThatCode(() -> new EmployeeRef(4L, "EMP-001", "Ana Ruiz")).doesNotThrowAnyException();
        assertThat(new EmployeeRef(4L, "EMP-001", "Ana Ruiz"))
                .isEqualTo(new EmployeeRef(4L, "EMP-001", "Ana Ruiz"));
    }
}
