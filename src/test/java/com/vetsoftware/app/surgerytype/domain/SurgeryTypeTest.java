package com.vetsoftware.app.surgerytype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SurgeryType")
class SurgeryTypeTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("create() nace sin id, habilitado y con la empresa recibida")
        void create_nace_sin_id_habilitado_y_con_la_empresa() {
            SurgeryType tipo = SurgeryType.create("Castracion", "Cirugia de esterilizacion",
                    SurgeryTypeMother.EMPRESA, false);

            assertThat(tipo.getId()).isNull();
            assertThat(tipo.getName()).isEqualTo("Castracion");
            assertThat(tipo.getDescription()).isEqualTo("Cirugia de esterilizacion");
            assertThat(tipo.getCompany()).isEqualTo(SurgeryTypeMother.EMPRESA);
            assertThat(tipo.isGeneral()).isFalse();
            assertThat(tipo.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(tipo.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("un tipo general nace sin empresa")
        void un_tipo_general_nace_sin_empresa() {
            SurgeryType tipo = SurgeryType.create("Cirugia general", "Procedimiento estandar", null,
                    true);

            assertThat(tipo.isGeneral()).isTrue();
            assertThat(tipo.getCompany()).isNull();
        }

        @Test
        @DisplayName("el constructor publico acepta todos los campos de persistencia")
        void el_constructor_acepta_todos_los_campos() {
            SurgeryType tipo = new SurgeryType(SurgeryTypeMother.SURGERY_TYPE_ID, "Castracion",
                    "Cirugia de esterilizacion", SurgeryTypeMother.EMPRESA, false,
                    SurgeryTypeMother.CREADO, null, false);

            assertThat(tipo.getId()).isEqualTo(SurgeryTypeMother.SURGERY_TYPE_ID);
            assertThat(tipo.getCreatedDate()).isEqualTo(SurgeryTypeMother.CREADO);
            assertThat(tipo.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("description es opcional: un tipo sin descripcion es valido")
        void description_es_opcional() {
            assertThatCode(() -> new SurgeryType(null, "Castracion", null,
                    SurgeryTypeMother.EMPRESA, false, LocalDateTime.now(), null, true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update reemplaza name, description, company y general; conserva id y createdDate")
        void update_reemplaza_los_campos_editables() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();

            tipo.update("Castracion avanzada", "Nueva descripcion", SurgeryTypeMother.OTRA_EMPRESA,
                    false);

            assertThat(tipo.getName()).isEqualTo("Castracion avanzada");
            assertThat(tipo.getDescription()).isEqualTo("Nueva descripcion");
            assertThat(tipo.getCompany()).isEqualTo(SurgeryTypeMother.OTRA_EMPRESA);
            assertThat(tipo.isGeneral()).isFalse();
            assertThat(tipo.getId()).isEqualTo(SurgeryTypeMother.SURGERY_TYPE_ID);
            assertThat(tipo.getCreatedDate()).isEqualTo(SurgeryTypeMother.CREADO);
        }

        @Test
        @DisplayName("update tambien valida los invariantes")
        void update_tambien_valida_los_invariantes() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();

            assertThatThrownBy(() -> tipo.update("", "desc", SurgeryTypeMother.EMPRESA, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();

            // El nombre es valido y la combinacion general/company no: si validate() no
            // corriera ANTES de asignar, el tipo se quedaria con el nombre nuevo y la
            // empresa vieja a medio actualizar.
            assertThatThrownBy(() -> tipo.update("Nuevo nombre", "desc", null, false))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(tipo.getName()).isEqualTo("Castracion");
            assertThat(tipo.getCompany()).isEqualTo(SurgeryTypeMother.EMPRESA);
        }

        @Test
        @DisplayName("update puede convertir un tipo propio en general quitando la empresa")
        void update_puede_convertir_en_general() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();

            tipo.update("Cirugia general", "desc", null, true);

            assertThat(tipo.isGeneral()).isTrue();
            assertThat(tipo.getCompany()).isNull();
        }
    }

    @Nested
    @DisplayName("Habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("enable marca el tipo como activo")
        void enable_marca_el_tipo_como_activo() {
            SurgeryType tipo = SurgeryTypeMother.deshabilitado();

            tipo.enable();

            assertThat(tipo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("disable marca el tipo como inactivo")
        void disable_marca_el_tipo_como_inactivo() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();

            tipo.disable();

            assertThat(tipo.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        private static SurgeryType construir(String name, String description, CompanyRef company,
                boolean general) {
            return new SurgeryType(null, name, description, company, general, LocalDateTime.now(),
                    null, true);
        }

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (ThrowingCallable) () -> construir(null, "d", SurgeryTypeMother.EMPRESA,
                                    false),
                            "name is required"),
                    arguments("name vacio",
                            (ThrowingCallable) () -> construir("", "d", SurgeryTypeMother.EMPRESA,
                                    false),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> construir("   ", "d",
                                    SurgeryTypeMother.EMPRESA, false),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> construir("x".repeat(101), "d",
                                    SurgeryTypeMother.EMPRESA, false),
                            "name must be 100 chars or less"),
                    arguments("description de 501 chars",
                            (ThrowingCallable) () -> construir("Castracion", "x".repeat(501),
                                    SurgeryTypeMother.EMPRESA, false),
                            "description must be 500 chars or less"),
                    arguments("general con empresa",
                            (ThrowingCallable) () -> construir("Cirugia general", "d",
                                    SurgeryTypeMother.EMPRESA, true),
                            "general type cannot have company"),
                    arguments("no general sin empresa",
                            (ThrowingCallable) () -> construir("Castracion", "d", null, false),
                            "non-general type requires company"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(
                    () -> construir("x".repeat(longitud), "d", SurgeryTypeMother.EMPRESA, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("description de exactamente 500 chars se acepta")
        void description_de_500_chars_se_acepta() {
            assertThatCode(() -> construir("Castracion", "x".repeat(500), SurgeryTypeMother.EMPRESA,
                    false)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("general sin empresa es el unico par coherente para un tipo global")
        void general_sin_empresa_es_coherente() {
            assertThatCode(() -> construir("Cirugia general", "d", null, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("no general con empresa es el unico par coherente para un tipo propio")
        void no_general_con_empresa_es_coherente() {
            assertThatCode(() -> construir("Castracion", "d", SurgeryTypeMother.EMPRESA, false))
                    .doesNotThrowAnyException();
        }
    }
}
