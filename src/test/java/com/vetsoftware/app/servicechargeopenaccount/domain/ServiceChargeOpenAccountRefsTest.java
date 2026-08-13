package com.vetsoftware.app.servicechargeopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
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
 * dejaran pasar un id null o un nombre vacio, el cargo se guardaria apuntando a
 * un agregado que no se puede mostrar, y el fallo aparece en la pantalla del
 * cliente, no aqui.
 */
@DisplayName("Companion VOs de servicechargeopenaccount")
class ServiceChargeOpenAccountRefsTest {

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("AnimalRef sin id",
                            (ThrowingCallable) () -> new AnimalRef(null, "Firulais", "A-001"),
                            "animal id is required"),
                    arguments("AnimalRef sin nombre",
                            (ThrowingCallable) () -> new AnimalRef(1L, null, "A-001"),
                            "animal name is required"),
                    arguments("AnimalRef con nombre en blanco",
                            (ThrowingCallable) () -> new AnimalRef(1L, "   ", "A-001"),
                            "animal name is required"),
                    arguments("EmployeeRef sin id",
                            (ThrowingCallable) () -> new EmployeeRef(null, "Ana"),
                            "employee id is required"),
                    arguments("EmployeeRef sin nombre",
                            (ThrowingCallable) () -> new EmployeeRef(1L, ""),
                            "employee name is required"),
                    arguments("OpenAccountRef sin id",
                            (ThrowingCallable) () -> new OpenAccountRef(null, 9L),
                            "openAccount id is required"),
                    arguments("OpenAccountRef sin empresa",
                            (ThrowingCallable) () -> new OpenAccountRef(50L, null),
                            "openAccount companyId is required"),
                    arguments("ServiceRef sin id",
                            (ThrowingCallable) () -> new ServiceRef(null, "Consulta",
                                    BigDecimal.TEN),
                            "service id is required"),
                    arguments("ServiceRef sin nombre",
                            (ThrowingCallable) () -> new ServiceRef(2L, "  ", BigDecimal.TEN),
                            "service name is required"),
                    arguments("ServiceRef sin precio",
                            (ThrowingCallable) () -> new ServiceRef(2L, "Consulta", null),
                            "service price is required"),
                    arguments("TaxRef sin id",
                            (ThrowingCallable) () -> new TaxRef(null, "IVA", BigDecimal.ONE),
                            "tax id is required"),
                    arguments("TaxRef sin nombre",
                            (ThrowingCallable) () -> new TaxRef(4L, null, BigDecimal.ONE),
                            "tax name is required"),
                    arguments("TaxRef sin porcentaje",
                            (ThrowingCallable) () -> new TaxRef(4L, "IVA", null),
                            "tax percentage is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza")
        void rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("campos opcionales y constructores compactos")
    class Opcionales {

        @Test
        @DisplayName("el codigo del animal es opcional")
        void el_codigo_del_animal_es_opcional() {
            assertThatCode(() -> new AnimalRef(1L, "Firulais", null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ServiceRef de 3 argumentos deja el servicio sin impuesto")
        void service_ref_de_tres_argumentos_deja_sin_impuesto() {
            ServiceRef ref = new ServiceRef(2L, "Bano", new BigDecimal("5000"));

            assertThat(ref.hasTax()).isFalse();
            assertThat(ref.tax()).isNull();
            assertThat(ref.taxTreatment()).isNull();
            assertThat(ref.price()).isEqualByComparingTo("5000");
        }

        @Test
        @DisplayName("TaxRef de 3 argumentos deja el esquema sin fijar")
        void tax_ref_de_tres_argumentos_deja_el_esquema_sin_fijar() {
            TaxRef ref = new TaxRef(4L, "IVA 19%", new BigDecimal("19.00"));

            assertThat(ref.scheme()).isNull();
            assertThat(ref.percentage()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("un impuesto del 0 % es valido: EXENTO no es lo mismo que sin impuesto")
        void un_impuesto_del_cero_por_ciento_es_valido() {
            assertThatCode(() -> new TaxRef(5L, "IVA 0%", BigDecimal.ZERO, "IVA"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("igualdad de valor")
    class Igualdad {

        @Test
        @DisplayName("dos refs con los mismos campos son la misma")
        void dos_refs_con_los_mismos_campos_son_la_misma() {
            assertThat(new AnimalRef(1L, "Firulais", "A-001"))
                    .isEqualTo(new AnimalRef(1L, "Firulais", "A-001"))
                    .hasSameHashCodeAs(new AnimalRef(1L, "Firulais", "A-001"));
            assertThat(new OpenAccountRef(50L, 9L)).isEqualTo(new OpenAccountRef(50L, 9L));
        }

        @Test
        @DisplayName("el id no basta: un nombre distinto es otra ref")
        void el_id_no_basta() {
            assertThat(new EmployeeRef(7L, "Ana Ruiz")).isNotEqualTo(new EmployeeRef(7L, "Otra"));
        }
    }
}
