package com.vetsoftware.app.service.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.service.testsupport.ServiceMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Service — invariantes y ciclo de vida del agregado")
class ServiceTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 13
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String name = "Consulta general";
        private BigDecimal price = new BigDecimal("50000.00");
        private TaxTreatment taxTreatment = TaxTreatment.GRAVADO;
        private String notes = "Consulta veterinaria estandar";
        private ServiceCategoryRef serviceCategory = ServiceMother.CONSULTAS;
        private TaxRef tax = ServiceMother.IVA_19;
        private CompanyRef company = ServiceMother.CLINICA;
        private LocalDateTime createdDate = ServiceMother.CREADO;
        private LocalDateTime updatedDate;
        private Long updatedBy;
        private Long version = 0L;
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder price(BigDecimal v) {
            this.price = v;
            return this;
        }

        private Builder taxTreatment(TaxTreatment v) {
            this.taxTreatment = v;
            return this;
        }

        private Builder notes(String v) {
            this.notes = v;
            return this;
        }

        private Builder serviceCategory(ServiceCategoryRef v) {
            this.serviceCategory = v;
            return this;
        }

        private Builder tax(TaxRef v) {
            this.tax = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Service build() {
            return new Service(id, name, price, taxTreatment, notes, serviceCategory, tax, company,
                    createdDate, updatedDate, updatedBy, version, enabled);
        }

        private void applyTo(Service service) {
            service.update(name, price, taxTreatment, notes, serviceCategory, tax, company, 77L,
                    5L);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Service service = valido().build();

            assertThat(service.getId()).isEqualTo(1L);
            assertThat(service.getName()).isEqualTo("Consulta general");
            assertThat(service.getPrice()).isEqualByComparingTo("50000.00");
            assertThat(service.getTaxTreatment()).isEqualTo(TaxTreatment.GRAVADO);
            assertThat(service.getNotes()).isEqualTo("Consulta veterinaria estandar");
            assertThat(service.getServiceCategory()).isEqualTo(ServiceMother.CONSULTAS);
            assertThat(service.getTax()).isEqualTo(ServiceMother.IVA_19);
            assertThat(service.getCompany()).isEqualTo(ServiceMother.CLINICA);
            assertThat(service.getCreatedDate()).isEqualTo(ServiceMother.CREADO);
            assertThat(service.getUpdatedDate()).isNull();
            assertThat(service.getUpdatedBy()).isNull();
            assertThat(service.getVersion()).isEqualTo(0L);
            assertThat(service.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha de hoy")
        void create_nace_sin_id_habilitado_y_con_la_fecha_de_hoy() {
            Service service = Service.create("Consulta general", new BigDecimal("50000.00"),
                    TaxTreatment.GRAVADO, "notas", ServiceMother.CONSULTAS, ServiceMother.IVA_19,
                    ServiceMother.CLINICA);

            assertThat(service.getId()).isNull();
            assertThat(service.isEnabled()).isTrue();
            assertThat(service.getUpdatedDate()).isNull();
            assertThat(service.getUpdatedBy()).isNull();
            assertThat(service.getVersion()).isNull();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(service.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("notes es opcional: un servicio sin notas es valido")
        void notes_es_opcional() {
            assertThatCode(() -> valido().notes(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("precio en cero se acepta")
        void precio_en_cero_se_acepta() {
            assertThatCode(() -> valido().price(BigDecimal.ZERO).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes de los campos base")
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
                    arguments("price null", (ThrowingCallable) () -> valido().price(null).build(),
                            "price is required"),
                    arguments("price negativo",
                            (ThrowingCallable) () -> valido().price(new BigDecimal("-0.01"))
                                    .build(),
                            "price cannot be negative"),
                    arguments("notes de 501 chars",
                            (ThrowingCallable) () -> valido().notes("x".repeat(501)).build(),
                            "notes must be 500 chars or less"),
                    arguments("serviceCategory null",
                            (ThrowingCallable) () -> valido().serviceCategory(null).build(),
                            "serviceCategory is required"),
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
        @org.junit.jupiter.params.provider.ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("notes de 500 chars se acepta")
        void notes_de_500_chars_se_acepta() {
            assertThatCode(() -> valido().notes("x".repeat(500)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("tratamiento tributario — matriz por TaxTreatment")
    class TratamientoTributario {

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"GRAVADO", "INC"})
        @DisplayName("GRAVADO/INC exige un impuesto")
        void gravado_o_inc_exige_un_impuesto(TaxTreatment treatment) {
            assertThatThrownBy(() -> valido().taxTreatment(treatment).tax(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment " + treatment + " requires a tax");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"GRAVADO", "INC"})
        @DisplayName("GRAVADO/INC exige un impuesto con tasa mayor que cero")
        void gravado_o_inc_exige_tasa_mayor_que_cero(TaxTreatment treatment) {
            TaxRef sinTasa = new TaxRef(30L, "Sin tasa", BigDecimal.ZERO);

            assertThatThrownBy(() -> valido().taxTreatment(treatment).tax(sinTasa).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment " + treatment
                            + " requires a tax percentage greater than 0");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"GRAVADO", "INC"})
        @DisplayName("GRAVADO/INC con impuesto valido no lanza")
        void gravado_o_inc_con_impuesto_valido_no_lanza(TaxTreatment treatment) {
            assertThatCode(() -> valido().taxTreatment(treatment).tax(ServiceMother.IVA_19).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"EXENTO", "EXCLUIDO"})
        @DisplayName("EXENTO/EXCLUIDO no admite impuesto")
        void exento_o_excluido_no_admite_impuesto(TaxTreatment treatment) {
            assertThatThrownBy(
                    () -> valido().taxTreatment(treatment).tax(ServiceMother.IVA_19).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment " + treatment + " must not have a tax");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"EXENTO", "EXCLUIDO"})
        @DisplayName("EXENTO/EXCLUIDO sin impuesto no lanza")
        void exento_o_excluido_sin_impuesto_no_lanza(TaxTreatment treatment) {
            assertThatCode(() -> valido().taxTreatment(treatment).tax(null).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("taxTreatment null se rechaza")
        void tax_treatment_null_se_rechaza() {
            assertThatThrownBy(() -> valido().taxTreatment(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment is required");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Service service = valido().build();
            ServiceCategoryRef otraCategoria = new ServiceCategoryRef(21L, "Cirugias");
            CompanyRef otraEmpresa = ServiceMother.CLINICA;

            service.update("Cirugia menor", new BigDecimal("120000.00"), TaxTreatment.EXCLUIDO,
                    null, otraCategoria, null, otraEmpresa, 77L, 5L);

            assertThat(service.getName()).isEqualTo("Cirugia menor");
            assertThat(service.getPrice()).isEqualByComparingTo("120000.00");
            assertThat(service.getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUIDO);
            assertThat(service.getNotes()).isNull();
            assertThat(service.getServiceCategory()).isEqualTo(otraCategoria);
            assertThat(service.getTax()).isNull();
            assertThat(service.getUpdatedBy()).isEqualTo(77L);
            assertThat(service.getVersion()).isEqualTo(5L);
            assertThat(service.getId()).isEqualTo(1L);
            assertThat(service.getCreatedDate()).isEqualTo(ServiceMother.CREADO);
            assertThat(service.getUpdatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Service service = valido().build();

            // El nombre es valido y la categoria no: si validate() no corriera ANTES de
            // asignar, el servicio se quedaria con el nombre nuevo y la categoria vieja.
            assertThatThrownBy(
                    () -> valido().name("Cirugia menor").serviceCategory(null).applyTo(service))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(service.getName()).isEqualTo("Consulta general");
            assertThat(service.getServiceCategory()).isEqualTo(ServiceMother.CONSULTAS);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Service service = valido().build();
            service.disable();

            valido().name("Cirugia menor").applyTo(service);

            assertThat(service.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Service service = valido().build();

            service.disable();
            assertThat(service.isEnabled()).isFalse();
            service.disable();
            assertThat(service.isEnabled()).isFalse();

            service.enable();
            assertThat(service.isEnabled()).isTrue();
            service.enable();
            assertThat(service.isEnabled()).isTrue();
        }
    }
}
