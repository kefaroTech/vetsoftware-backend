package com.vetsoftware.app.promotion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.promotion.testsupport.PromotionMother;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Promotion — invariantes de vigencia, valor y ciclo de vida")
class PromotionTest {

    /**
     * Constructor de fixtures con un campo variable por caso, igual que en
     * AnimalTest: evita repetir trece argumentos por cada escenario invalido.
     */
    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = PromotionMother.PROMOTION_ID;
        private String name = "Enero perruno";
        private PromotionType promotionType = PromotionType.DISCOUNT;
        private ApplicationType applicationType = ApplicationType.CATEGORY;
        private Long applicationItem = PromotionMother.CATEGORY_ID;
        private ValueType valueType = ValueType.PERCENTAGE;
        private BigDecimal value = new BigDecimal("15.00");
        private LocalDateTime startDate = PromotionMother.INICIO;
        private LocalDateTime endDate = PromotionMother.FIN;
        private PromotionStatus promotionStatus = PromotionStatus.ACTIVE;
        private CompanyRef company = PromotionMother.CLINICA;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder promotionType(PromotionType v) {
            this.promotionType = v;
            return this;
        }

        private Builder applicationType(ApplicationType v) {
            this.applicationType = v;
            return this;
        }

        private Builder applicationItem(Long v) {
            this.applicationItem = v;
            return this;
        }

        private Builder valueType(ValueType v) {
            this.valueType = v;
            return this;
        }

        private Builder value(BigDecimal v) {
            this.value = v;
            return this;
        }

        private Builder startDate(LocalDateTime v) {
            this.startDate = v;
            return this;
        }

        private Builder endDate(LocalDateTime v) {
            this.endDate = v;
            return this;
        }

        private Builder promotionStatus(PromotionStatus v) {
            this.promotionStatus = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Promotion build() {
            return new Promotion(id, name, promotionType, applicationType, applicationItem,
                    valueType, value, startDate, endDate, promotionStatus, company,
                    PromotionMother.CREADA, true);
        }

        private void applyTo(Promotion promotion) {
            promotion.update(name, promotionType, applicationType, applicationItem, valueType,
                    value, startDate, endDate, promotionStatus, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Promotion promotion = valida().build();

            assertThat(promotion.getId()).isEqualTo(PromotionMother.PROMOTION_ID);
            assertThat(promotion.getName()).isEqualTo("Enero perruno");
            assertThat(promotion.getPromotionType()).isEqualTo(PromotionType.DISCOUNT);
            assertThat(promotion.getApplicationType()).isEqualTo(ApplicationType.CATEGORY);
            assertThat(promotion.getApplicationItem()).isEqualTo(PromotionMother.CATEGORY_ID);
            assertThat(promotion.getValueType()).isEqualTo(ValueType.PERCENTAGE);
            assertThat(promotion.getValue()).isEqualByComparingTo("15.00");
            assertThat(promotion.getStartDate()).isEqualTo(PromotionMother.INICIO);
            assertThat(promotion.getEndDate()).isEqualTo(PromotionMother.FIN);
            assertThat(promotion.getPromotionStatus()).isEqualTo(PromotionStatus.ACTIVE);
            assertThat(promotion.getCompany()).isEqualTo(PromotionMother.CLINICA);
            assertThat(promotion.getCreatedDate()).isEqualTo(PromotionMother.CREADA);
            assertThat(promotion.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y con fecha de creacion actual")
        void create_nace_sin_id_habilitada_y_con_fecha_de_creacion_actual() {
            Promotion promotion = Promotion.create("Enero perruno", PromotionType.DISCOUNT,
                    ApplicationType.CATEGORY, PromotionMother.CATEGORY_ID, ValueType.PERCENTAGE,
                    new BigDecimal("15.00"), PromotionMother.INICIO, PromotionMother.FIN,
                    PromotionStatus.ACTIVE, PromotionMother.CLINICA);

            assertThat(promotion.getId()).isNull();
            assertThat(promotion.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(promotion.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valida().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valida().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valida().name("   ").build(),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valida().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("promotionType null",
                            (ThrowingCallable) () -> valida().promotionType(null).build(),
                            "promotionType is required"),
                    arguments("applicationType null",
                            (ThrowingCallable) () -> valida().applicationType(null).build(),
                            "applicationType is required"),
                    arguments("applicationItem null",
                            (ThrowingCallable) () -> valida().applicationItem(null).build(),
                            "applicationItem is required"),
                    arguments("applicationItem cero",
                            (ThrowingCallable) () -> valida().applicationItem(0L).build(),
                            "applicationItem is required"),
                    arguments("applicationItem negativo",
                            (ThrowingCallable) () -> valida().applicationItem(-1L).build(),
                            "applicationItem is required"),
                    arguments("valueType null",
                            (ThrowingCallable) () -> valida().valueType(null).build(),
                            "valueType is required"),
                    arguments("value null", (ThrowingCallable) () -> valida().value(null).build(),
                            "value is required"),
                    arguments("value negativo",
                            (ThrowingCallable) () -> valida().value(new BigDecimal("-0.01"))
                                    .build(),
                            "value cannot be negative"),
                    arguments("porcentaje mayor a 100",
                            (ThrowingCallable) () -> valida().valueType(ValueType.PERCENTAGE)
                                    .value(new BigDecimal("100.01")).build(),
                            "percentage value cannot be greater than 100"),
                    arguments("startDate null",
                            (ThrowingCallable) () -> valida().startDate(null).build(),
                            "startDate is required"),
                    arguments("endDate null",
                            (ThrowingCallable) () -> valida().endDate(null).build(),
                            "endDate is required"),
                    arguments("endDate antes de startDate",
                            (ThrowingCallable) () -> valida()
                                    .startDate(LocalDateTime.of(2026, 1, 31, 23, 59))
                                    .endDate(LocalDateTime.of(2026, 1, 1, 0, 0)).build(),
                            "endDate cannot be before startDate"),
                    arguments("promotionStatus null",
                            (ThrowingCallable) () -> valida().promotionStatus(null).build(),
                            "promotionStatus is required"),
                    arguments("company null",
                            (ThrowingCallable) () -> valida().company(null).build(),
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
            assertThatCode(() -> valida().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("applicationItem en 1 es el limite valido mas bajo")
        void application_item_en_uno_es_valido() {
            assertThatCode(() -> valida().applicationItem(1L).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("value en cero no es negativo y se acepta")
        void value_en_cero_se_acepta() {
            assertThatCode(() -> valida().value(BigDecimal.ZERO).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("porcentaje exactamente en 100 es el limite valido mas alto")
        void porcentaje_en_cien_se_acepta() {
            assertThatCode(() -> valida().valueType(ValueType.PERCENTAGE)
                    .value(new BigDecimal("100")).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("un valor fijo mayor a 100 no esta limitado por la regla de porcentaje")
        void valor_fijo_mayor_a_cien_se_acepta() {
            assertThatCode(() -> valida().valueType(ValueType.VALUE).value(new BigDecimal("500000"))
                    .build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("endDate igual a startDate es el limite valido: no es 'antes'")
        void end_date_igual_a_start_date_se_acepta() {
            assertThatCode(() -> valida().startDate(PromotionMother.INICIO)
                    .endDate(PromotionMother.INICIO).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Promotion promotion = valida().build();

            valida().name("Febrero felino").promotionType(PromotionType.SPECIAL_PRICE)
                    .applicationType(ApplicationType.PRODUCT).applicationItem(7L)
                    .valueType(ValueType.VALUE).value(new BigDecimal("8000.00"))
                    .promotionStatus(PromotionStatus.INACTIVE).company(PromotionMother.OTRA_CLINICA)
                    .applyTo(promotion);

            assertThat(promotion.getName()).isEqualTo("Febrero felino");
            assertThat(promotion.getPromotionType()).isEqualTo(PromotionType.SPECIAL_PRICE);
            assertThat(promotion.getApplicationType()).isEqualTo(ApplicationType.PRODUCT);
            assertThat(promotion.getApplicationItem()).isEqualTo(7L);
            assertThat(promotion.getValueType()).isEqualTo(ValueType.VALUE);
            assertThat(promotion.getValue()).isEqualByComparingTo("8000.00");
            assertThat(promotion.getPromotionStatus()).isEqualTo(PromotionStatus.INACTIVE);
            assertThat(promotion.getCompany()).isEqualTo(PromotionMother.OTRA_CLINICA);
            assertThat(promotion.getId()).isEqualTo(PromotionMother.PROMOTION_ID);
            assertThat(promotion.getCreatedDate()).isEqualTo(PromotionMother.CREADA);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Promotion promotion = valida().build();

            // El nombre es valido y el valueType no: si validate() no corriera ANTES de
            // asignar, la promocion se quedaria con el nombre nuevo y el valueType viejo.
            assertThatThrownBy(
                    () -> valida().name("Febrero felino").valueType(null).applyTo(promotion))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(promotion.getName()).isEqualTo("Enero perruno");
            assertThat(promotion.getValueType()).isEqualTo(ValueType.PERCENTAGE);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Promotion promotion = valida().build();
            promotion.disable();

            valida().name("Febrero felino").applyTo(promotion);

            assertThat(promotion.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Promotion promotion = valida().build();

            promotion.disable();
            assertThat(promotion.isEnabled()).isFalse();
            promotion.disable();
            assertThat(promotion.isEnabled()).isFalse();

            promotion.enable();
            assertThat(promotion.isEnabled()).isTrue();
            promotion.enable();
            assertThat(promotion.isEnabled()).isTrue();
        }
    }
}
