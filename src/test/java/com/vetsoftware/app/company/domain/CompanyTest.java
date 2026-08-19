package com.vetsoftware.app.company.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Company — invariantes y ciclo de vida del agregado")
class CompanyTest {

    private static final String NOMBRE_100 = "n".repeat(100);
    private static final String NOMBRE_101 = "n".repeat(101);
    private static final String IDENTIFICADOR_50 = "i".repeat(50);
    private static final String IDENTIFICADOR_51 = "i".repeat(51);

    /**
     * Construye una empresa dejando variar solo los cuatro campos con invariante.
     * Evita repetir nueve argumentos por escenario, que es como se cuela un test
     * que valida un campo distinto del que dice validar.
     */
    private static Company nueva(String name, String identifier, CityRef city,
            MembershipRef membership) {
        return new Company(1L, name, identifier, "Calle 123", "3001234567", city, membership,
                CompanyMother.CREADO, null, true);
    }

    private static void actualizar(Company company, String name, String identifier, CityRef city,
            MembershipRef membership) {
        company.update(name, identifier, "Calle 123", "3001234567", city, membership);
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("acepta los campos validos y los expone tal cual")
        void acepta_los_campos_validos() {
            Company company = new Company(7L, "Clinica Norte", "NIT-900", "Calle 123 #45-67",
                    "3001234567", CompanyMother.BOGOTA, CompanyMother.PREMIUM, CompanyMother.CREADO,
                    null, true);

            assertThat(company.getId()).isEqualTo(7L);
            assertThat(company.getName()).isEqualTo("Clinica Norte");
            assertThat(company.getIdentifier()).isEqualTo("NIT-900");
            assertThat(company.getAddress()).isEqualTo("Calle 123 #45-67");
            assertThat(company.getContactNumber()).isEqualTo("3001234567");
            assertThat(company.getCity()).isEqualTo(CompanyMother.BOGOTA);
            assertThat(company.getMembership()).isEqualTo(CompanyMother.PREMIUM);
            assertThat(company.getCreatedDate()).isEqualTo(CompanyMother.CREADO);
            assertThat(company.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("direccion y telefono son opcionales: admite null en ambos")
        void direccion_y_telefono_son_opcionales() {
            Company company = new Company(7L, "Clinica Norte", "NIT-900", null, null,
                    CompanyMother.BOGOTA, CompanyMother.PREMIUM, CompanyMother.CREADO, null, true);

            assertThat(company.getAddress()).isNull();
            assertThat(company.getContactNumber()).isNull();
        }

        @Test
        @DisplayName("admite id nulo: la empresa aun no persistida es valida")
        void admite_id_nulo() {
            Company company = new Company(null, "Clinica Norte", "NIT-900", null, null,
                    CompanyMother.BOGOTA, CompanyMother.PREMIUM, CompanyMother.CREADO, null, true);

            assertThat(company.getId()).isNull();
            assertThat(company.getName()).isEqualTo("Clinica Norte");
        }

        static Stream<Arguments> camposInvalidos() {
            return Stream.of(
                    arguments("nombre nulo",
                            (ThrowingCallable) () -> nueva(null, "NIT-900", CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "name is required"),
                    arguments("nombre vacio",
                            (ThrowingCallable) () -> nueva("", "NIT-900", CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "name is required"),
                    arguments("nombre en blanco",
                            (ThrowingCallable) () -> nueva("   ", "NIT-900", CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "name is required"),
                    arguments("nombre de 101 caracteres",
                            (ThrowingCallable) () -> nueva(NOMBRE_101, "NIT-900",
                                    CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "name must be 100 chars or less"),
                    arguments("identificador nulo",
                            (ThrowingCallable) () -> nueva("Clinica Norte", null,
                                    CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "identifier is required"),
                    arguments("identificador vacio",
                            (ThrowingCallable) () -> nueva("Clinica Norte", "",
                                    CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "identifier is required"),
                    arguments("identificador en blanco",
                            (ThrowingCallable) () -> nueva("Clinica Norte", "  ",
                                    CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "identifier is required"),
                    arguments("identificador de 51 caracteres",
                            (ThrowingCallable) () -> nueva("Clinica Norte", IDENTIFICADOR_51,
                                    CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "identifier must be 50 chars or less"),
                    arguments("ciudad nula",
                            (ThrowingCallable) () -> nueva("Clinica Norte", "NIT-900", null,
                                    CompanyMother.PREMIUM),
                            "city is required"),
                    arguments(
                            "membresia nula", (ThrowingCallable) () -> nueva("Clinica Norte",
                                    "NIT-900", CompanyMother.BOGOTA, null),
                            "membership is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("camposInvalidos")
        @DisplayName("rechaza el agregado con un campo obligatorio invalido")
        void rechaza_el_agregado_con_un_campo_invalido(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta el nombre en el limite exacto de 100 caracteres")
        void acepta_el_nombre_en_el_limite_de_100() {
            assertThatCode(
                    () -> nueva(NOMBRE_100, "NIT-900", CompanyMother.BOGOTA, CompanyMother.PREMIUM))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta el identificador en el limite exacto de 50 caracteres")
        void acepta_el_identificador_en_el_limite_de_50() {
            assertThatCode(() -> nueva("Clinica Norte", IDENTIFICADOR_50, CompanyMother.BOGOTA,
                    CompanyMother.PREMIUM)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("factory create")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitada y con fecha de creacion asignada")
        void nace_sin_id_habilitada_y_con_fecha() {
            Company company = Company.create("Clinica Norte", "NIT-900", "Calle 123", "3001234567",
                    CompanyMother.BOGOTA, CompanyMother.PREMIUM);

            assertThat(company.getId()).isNull();
            assertThat(company.isEnabled()).isTrue();
            assertThat(company.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("copia cada argumento en su campo sin cruzarlos")
        void copia_cada_argumento_en_su_campo() {
            Company company = Company.create("Clinica Norte", "NIT-900", "Calle 123", "3001234567",
                    CompanyMother.BOGOTA, CompanyMother.PREMIUM);

            assertThat(company.getName()).isEqualTo("Clinica Norte");
            assertThat(company.getIdentifier()).isEqualTo("NIT-900");
            assertThat(company.getAddress()).isEqualTo("Calle 123");
            assertThat(company.getContactNumber()).isEqualTo("3001234567");
            assertThat(company.getCity()).isEqualTo(CompanyMother.BOGOTA);
            assertThat(company.getMembership()).isEqualTo(CompanyMother.PREMIUM);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("el factory aplica las mismas invariantes que el constructor")
        void el_factory_aplica_las_mismas_invariantes(String nombre) {
            assertThatThrownBy(() -> Company.create(nombre, "NIT-900", null, null,
                    CompanyMother.BOGOTA, CompanyMother.PREMIUM))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("update")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza los campos mutables incluidas las dos referencias")
        void reemplaza_los_campos_mutables() {
            Company company = CompanyMother.clinicaNorte();

            company.update("Clinica Sur", "NIT-901", "Carrera 45", "3009876543",
                    CompanyMother.MEDELLIN, CompanyMother.BASICA);

            assertThat(company.getName()).isEqualTo("Clinica Sur");
            assertThat(company.getIdentifier()).isEqualTo("NIT-901");
            assertThat(company.getAddress()).isEqualTo("Carrera 45");
            assertThat(company.getContactNumber()).isEqualTo("3009876543");
            assertThat(company.getCity()).isEqualTo(CompanyMother.MEDELLIN);
            assertThat(company.getMembership()).isEqualTo(CompanyMother.BASICA);
        }

        @Test
        @DisplayName("no toca id, fecha de creacion ni el estado habilitado")
        void no_toca_id_fecha_ni_estado() {
            Company company = CompanyMother.deshabilitada();

            company.update("Clinica Sur", "NIT-901", "Carrera 45", "3009876543",
                    CompanyMother.MEDELLIN, CompanyMother.BASICA);

            assertThat(company.getId()).isEqualTo(CompanyMother.COMPANY_ID);
            assertThat(company.getCreatedDate()).isEqualTo(CompanyMother.CREADO);
            assertThat(company.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("permite borrar direccion y telefono pasando null")
        void permite_borrar_direccion_y_telefono() {
            Company company = CompanyMother.clinicaNorte();

            company.update("Clinica Norte", "NIT-900", null, null, CompanyMother.BOGOTA,
                    CompanyMother.PREMIUM);

            assertThat(company.getAddress()).isNull();
            assertThat(company.getContactNumber()).isNull();
        }

        static Stream<Arguments> actualizacionesInvalidas() {
            return Stream.of(
                    arguments("nombre nulo",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(), null,
                                    "NIT-900", CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "name is required"),
                    arguments("nombre en blanco",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(), "  ",
                                    "NIT-900", CompanyMother.BOGOTA, CompanyMother.PREMIUM),
                            "name is required"),
                    arguments("nombre de 101 caracteres",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(),
                                    NOMBRE_101, "NIT-900", CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "name must be 100 chars or less"),
                    arguments("identificador nulo",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(),
                                    "Clinica Sur", null, CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "identifier is required"),
                    arguments("identificador en blanco",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(),
                                    "Clinica Sur", " ", CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "identifier is required"),
                    arguments("identificador de 51 caracteres",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(),
                                    "Clinica Sur", IDENTIFICADOR_51, CompanyMother.BOGOTA,
                                    CompanyMother.PREMIUM),
                            "identifier must be 50 chars or less"),
                    arguments("ciudad nula",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(),
                                    "Clinica Sur", "NIT-901", null, CompanyMother.PREMIUM),
                            "city is required"),
                    arguments("membresia nula",
                            (ThrowingCallable) () -> actualizar(CompanyMother.clinicaNorte(),
                                    "Clinica Sur", "NIT-901", CompanyMother.BOGOTA, null),
                            "membership is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("actualizacionesInvalidas")
        @DisplayName("rechaza la actualizacion con un campo obligatorio invalido")
        void rechaza_la_actualizacion_invalida(String caso, ThrowingCallable actualizacion,
                String mensaje) {
            assertThatThrownBy(actualizacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta los limites exactos de 100 y 50 caracteres")
        void acepta_los_limites_exactos() {
            Company company = CompanyMother.clinicaNorte();

            assertThatCode(() -> actualizar(company, NOMBRE_100, IDENTIFICADOR_50,
                    CompanyMother.MEDELLIN, CompanyMother.BASICA)).doesNotThrowAnyException();
            assertThat(company.getName()).isEqualTo(NOMBRE_100);
            assertThat(company.getIdentifier()).isEqualTo(IDENTIFICADOR_50);
        }

        @Test
        @DisplayName("una actualizacion invalida deja el agregado intacto")
        void una_actualizacion_invalida_deja_el_agregado_intacto() {
            Company company = CompanyMother.clinicaNorte();

            assertThatThrownBy(() -> company.update("Clinica Sur", "NIT-901", "Carrera 45",
                    "3009876543", CompanyMother.MEDELLIN, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(company.getName()).isEqualTo("Clinica Norte");
            assertThat(company.getIdentifier()).isEqualTo("NIT-900");
            assertThat(company.getCity()).isEqualTo(CompanyMother.BOGOTA);
            assertThat(company.getMembership()).isEqualTo(CompanyMother.PREMIUM);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable apaga la empresa habilitada")
        void disable_apaga_la_empresa() {
            Company company = CompanyMother.clinicaNorte();

            company.disable();

            assertThat(company.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable reactiva la empresa deshabilitada")
        void enable_reactiva_la_empresa() {
            Company company = CompanyMother.deshabilitada();

            company.enable();

            assertThat(company.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("las transiciones son idempotentes: repetirlas no cambia el estado")
        void las_transiciones_son_idempotentes() {
            Company company = CompanyMother.clinicaNorte();

            company.enable();
            assertThat(company.isEnabled()).isTrue();

            company.disable();
            company.disable();
            assertThat(company.isEnabled()).isFalse();
        }
    }
}
