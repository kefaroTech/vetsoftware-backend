package com.vetsoftware.app.systemuserpermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SystemPermissionRef — invariantes")
class SystemPermissionRefTest {

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                arguments("id nulo", null, "Reportes", "reports.manage",
                        "system permission id is required"),
                arguments("name nulo", 8L, null, "reports.manage",
                        "system permission name is required"),
                arguments("name en blanco", 8L, "   ", "reports.manage",
                        "system permission name is required"),
                arguments("code nulo", 8L, "Reportes", null, "system permission code is required"),
                arguments("code en blanco", 8L, "Reportes", "   ",
                        "system permission code is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    @DisplayName("rechaza")
    void rechaza(String caso, Long id, String name, String code, String mensaje) {
        assertThatThrownBy(() -> new SystemPermissionRef(id, name, code))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
    }

    @Test
    @DisplayName("con datos validos no lanza y conserva los campos")
    void con_datos_validos_no_lanza_y_conserva_los_campos() {
        assertThatCode(() -> new SystemPermissionRef(8L, "Reportes", "reports.manage"))
                .doesNotThrowAnyException();

        SystemPermissionRef ref = new SystemPermissionRef(8L, "Reportes", "reports.manage");
        assertThat(ref.id()).isEqualTo(8L);
        assertThat(ref.name()).isEqualTo("Reportes");
        assertThat(ref.code()).isEqualTo("reports.manage");
    }
}
