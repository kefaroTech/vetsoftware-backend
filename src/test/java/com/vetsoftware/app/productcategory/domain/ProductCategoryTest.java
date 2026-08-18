package com.vetsoftware.app.productcategory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.productcategory.testsupport.ProductCategoryMother;
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

@DisplayName("ProductCategory — invariantes y ciclo de vida del agregado")
class ProductCategoryTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir nueve
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = ProductCategoryMother.CATEGORY_ID;
        private String name = "Medicamentos";
        private String description = "Categoria de medicamentos";
        private CompanyRef company = ProductCategoryMother.CLINICA;

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

        private ProductCategory build() {
            return new ProductCategory(id, name, description, company, ProductCategoryMother.CREADO,
                    null, null, 0L, true);
        }

        private void applyTo(ProductCategory productCategory) {
            productCategory.update(name, description, company, ProductCategoryMother.EMPLOYEE_ID,
                    1L);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            ProductCategory categoria = valido().build();

            assertThat(categoria.getId()).isEqualTo(ProductCategoryMother.CATEGORY_ID);
            assertThat(categoria.getName()).isEqualTo("Medicamentos");
            assertThat(categoria.getDescription()).isEqualTo("Categoria de medicamentos");
            assertThat(categoria.getCompany()).isEqualTo(ProductCategoryMother.CLINICA);
            assertThat(categoria.getCreatedDate()).isEqualTo(ProductCategoryMother.CREADO);
            assertThat(categoria.getUpdatedDate()).isNull();
            assertThat(categoria.getUpdatedBy()).isNull();
            assertThat(categoria.getVersion()).isZero();
            assertThat(categoria.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y sin fecha de actualizacion")
        void create_nace_sin_id_habilitada_y_sin_fecha_de_actualizacion() {
            ProductCategory categoria = ProductCategory.create("Medicamentos",
                    "Categoria de medicamentos", ProductCategoryMother.CLINICA);

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
            ProductCategory categoria = valido().build();

            valido().name("Insumos").description("Categoria de insumos")
                    .company(ProductCategoryMother.OTRA_CLINICA).applyTo(categoria);

            assertThat(categoria.getName()).isEqualTo("Insumos");
            assertThat(categoria.getDescription()).isEqualTo("Categoria de insumos");
            assertThat(categoria.getCompany()).isEqualTo(ProductCategoryMother.OTRA_CLINICA);
            assertThat(categoria.getId()).isEqualTo(ProductCategoryMother.CATEGORY_ID);
            assertThat(categoria.getCreatedDate()).isEqualTo(ProductCategoryMother.CREADO);
            assertThat(categoria.getUpdatedBy()).isEqualTo(ProductCategoryMother.EMPLOYEE_ID);
            assertThat(categoria.getVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("sella la fecha de actualizacion")
        void sella_la_fecha_de_actualizacion() {
            ProductCategory categoria = valido().build();

            valido().applyTo(categoria);

            assertThat(categoria.getUpdatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            ProductCategory categoria = valido().build();

            // El nombre es valido y la compania no: si validate() no corriera ANTES de
            // asignar, la categoria se quedaria con el nombre nuevo y la compania vieja.
            assertThatThrownBy(() -> valido().name("Insumos").company(null).applyTo(categoria))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(categoria.getName()).isEqualTo("Medicamentos");
            assertThat(categoria.getCompany()).isEqualTo(ProductCategoryMother.CLINICA);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            ProductCategory categoria = valido().build();
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
            ProductCategory categoria = valido().build();

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
