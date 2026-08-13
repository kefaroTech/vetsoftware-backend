package com.vetsoftware.app.debtopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Companion VOs de la feature. Son la frontera con las otras features: si
 * dejaran pasar un id null, el abono se guardaria apuntando a un agregado que
 * no se puede mostrar, y el fallo aparece en la pantalla del cliente, no aqui.
 */
@DisplayName("Companion VOs de debtopenaccount")
class DebtOpenAccountRefsTest {

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("EmployeeRef sin id",
                            (ThrowingCallable) () -> new EmployeeRef(null, "Ana"),
                            "employee id is required"),
                    arguments("OpenAccountRef sin id",
                            (ThrowingCallable) () -> new OpenAccountRef(null, 9L),
                            "open account id is required"),
                    arguments("OpenAccountRef sin empresa",
                            (ThrowingCallable) () -> new OpenAccountRef(50L, null),
                            "open account companyId is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza")
        void rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("el nombre del empleado es opcional, a diferencia de los otros modulos")
        void el_nombre_del_empleado_es_opcional() {
            // Aqui EmployeeRef solo exige el id: los abonos historicos pueden venir de
            // empleados sin nombre cargado. Es una diferencia real con
            // servicechargeopenaccount y generalchargeopenaccount, no un descuido; si
            // alguien la endurece, este test lo señala.
            assertThatCode(() -> new EmployeeRef(7L, null)).doesNotThrowAnyException();
            assertThatCode(() -> new EmployeeRef(7L, "   ")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("igualdad de valor")
    class Igualdad {

        @Test
        @DisplayName("dos refs con los mismos campos son la misma")
        void dos_refs_con_los_mismos_campos_son_la_misma() {
            assertThat(new OpenAccountRef(50L, 9L)).isEqualTo(new OpenAccountRef(50L, 9L))
                    .hasSameHashCodeAs(new OpenAccountRef(50L, 9L));
            assertThat(new EmployeeRef(7L, "Ana Ruiz")).isEqualTo(new EmployeeRef(7L, "Ana Ruiz"));
        }

        @Test
        @DisplayName("la misma cuenta en otra empresa no es la misma ref")
        void la_misma_cuenta_en_otra_empresa_no_es_la_misma_ref() {
            // El companyId es la mitad de la identidad: sin el, un guard de tenancy que
            // compare refs dejaria pasar la cuenta de otro tenant con el mismo id.
            assertThat(new OpenAccountRef(50L, 9L)).isNotEqualTo(new OpenAccountRef(50L, 99L));
        }
    }
}
