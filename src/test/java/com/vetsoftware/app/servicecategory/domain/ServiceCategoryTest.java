package com.vetsoftware.app.servicecategory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
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

@DisplayName("ServiceCategory — invariantes y ciclo de vida del agregado")
class ServiceCategoryTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir nueve
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = ServiceCategoryMother.CATEGORY_ID;
        private String name = "Consultas";
        private String description = "Categoria de consultas";
        private CompanyRef company = ServiceCategoryMother.CLINICA;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder description(String v) {
            this.description = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private ServiceCategory build() {
            return new ServiceCategory(id, name, description, company, ServiceCategoryMother.CREADO,
                    null, null, 0L, true);
        }

        private void applyTo(ServiceCategory serviceCategory) {
            serviceCategory.update(name, description, company, ServiceCategoryMother.EMPLOYEE_ID,
                    1L);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            ServiceCategory categoria = valido().build();

            assertThat(categoria.getId()).isEqualTo(ServiceCategoryMother.CATEGORY_ID);
            assertThat(categoria.getName()).isEqualTo("Consultas");
            assertThat(categoria.getDescription()).isEqualTo("Categoria de consultas");
            assertThat(categoria.getCompany()).isEqualTo(ServiceCategoryMother.CLINICA);
            assertThat(categoria.getCreatedDate()).isEqualTo(ServiceCategoryMother.CREADO);
            assertThat(categoria.getUpdatedDate()).isNull();
            assertThat(categoria.getUpdatedBy()).isNull();
            assertThat(categoria.getVersion()).isZero();
            assertThat(categoria.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y sin fecha de actualizacion")
        void create_nace_sin_id_habilitada_y_sin_fecha_de_actualizacion() {
            ServiceCategory categoria = ServiceCategory.create("Consultas",
                    "Categoria de consultas", ServiceCategoryMother.CLINICA);

            assertThat(categoria.getId()).isNull();
            assertThat(categoria.isEnabled()).isTrue();
            assertThat(categoria.getUpdatedDate()).isNull();
            assertThat(categoria.getUpdatedBy()).isNull();
            assertThat(categoria.getVersion()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(categoria.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valido().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("description null",
                            (ThrowingCallable) () -> valido().description(null).build(),
                            "description is required"),
                    arguments("description vacia",
                            (ThrowingCallable) () -> valido().description("").build(),
                            "description is required"),
                    arguments("description en blanco",
                            (ThrowingCallable) () -> valido().description("   ").build(),
                            "description is required"),
                    arguments("description de 501 chars",
                            (ThrowingCallable) () -> valido().description("x".repeat(501)).build(),
                            "description must be 500 chars or less"),
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"));
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
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 500})
        @DisplayName("description en el limite exacto se acepta")
        void description_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().description("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            ServiceCategory categoria = valido().build();

            valido().name("Cirugias").description("Categoria de cirugias")
                    .company(ServiceCategoryMother.OTRA_CLINICA).applyTo(categoria);

            assertThat(categoria.getName()).isEqualTo("Cirugias");
            assertThat(categoria.getDescription()).isEqualTo("Categoria de cirugias");
            assertThat(categoria.getCompany()).isEqualTo(ServiceCategoryMother.OTRA_CLINICA);
            assertThat(categoria.getId()).isEqualTo(ServiceCategoryMother.CATEGORY_ID);
            assertThat(categoria.getCreatedDate()).isEqualTo(ServiceCategoryMother.CREADO);
            assertThat(categoria.getUpdatedBy()).isEqualTo(ServiceCategoryMother.EMPLOYEE_ID);
            assertThat(categoria.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("sella la fecha de actualizacion")
        void sella_la_fecha_de_actualizacion() {
            ServiceCategory categoria = valido().build();

            valido().applyTo(categoria);

            assertThat(categoria.getUpdatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            ServiceCategory categoria = valido().build();

            // El nombre es valido y la compania no: si validate() no corriera ANTES de
            // asignar, la categoria se quedaria con el nombre nuevo y la compania vieja.
            assertThatThrownBy(() -> valido().name("Cirugias").company(null).applyTo(categoria))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(categoria.getName()).isEqualTo("Consultas");
            assertThat(categoria.getCompany()).isEqualTo(ServiceCategoryMother.CLINICA);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            ServiceCategory categoria = valido().build();
            categoria.disable();

            valido().applyTo(categoria);

            assertThat(categoria.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            ServiceCategory categoria = valido().build();

            categoria.disable();
            assertThat(categoria.isEnabled()).isFalse();
            categoria.disable();
            assertThat(categoria.isEnabled()).isFalse();

            categoria.enable();
            assertThat(categoria.isEnabled()).isTrue();
            categoria.enable();
            assertThat(categoria.isEnabled()).isTrue();
        }
    }
}
