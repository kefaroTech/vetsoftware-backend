package com.vetsoftware.app.tax.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.tax.testsupport.TaxMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Tax")
class TaxTest {

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("create sella la fecha de creacion, queda activo y en version nula")
        void create_sella_la_fecha_y_queda_activo() {
            Tax tax = Tax.create("IVA General", new BigDecimal("19.00"), TaxScheme.IVA,
                    TaxMother.EMPRESA);

            assertThat(tax.getId()).isNull();
            assertThat(tax.getName()).isEqualTo("IVA General");
            assertThat(tax.getPercentage()).isEqualByComparingTo("19.00");
            assertThat(tax.getTaxScheme()).isEqualTo(TaxScheme.IVA);
            assertThat(tax.getCompany()).isEqualTo(TaxMother.EMPRESA);
            assertThat(tax.getCreatedDate()).isNotNull();
            assertThat(tax.getUpdatedDate()).isNull();
            assertThat(tax.getVersion()).isNull();
            assertThat(tax.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el constructor publico acepta todos los campos de persistencia")
        void el_constructor_acepta_todos_los_campos() {
            Tax tax = new Tax(TaxMother.TAX_ID, "IVA General", new BigDecimal("19.00"),
                    TaxScheme.IVA, TaxMother.EMPRESA, TaxMother.CREADO, TaxMother.ACTUALIZADO,
                    TaxMother.EMPLOYEE_ID, 3L, false);

            assertThat(tax.getId()).isEqualTo(TaxMother.TAX_ID);
            assertThat(tax.getCreatedDate()).isEqualTo(TaxMother.CREADO);
            assertThat(tax.getUpdatedDate()).isEqualTo(TaxMother.ACTUALIZADO);
            assertThat(tax.getUpdatedBy()).isEqualTo(TaxMother.EMPLOYEE_ID);
            assertThat(tax.getVersion()).isEqualTo(3L);
            assertThat(tax.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update reemplaza los campos editables y sella version y autor")
        void update_reemplaza_los_campos_editables() {
            Tax tax = TaxMother.activo();

            tax.update("IVA Reducido", new BigDecimal("5.00"), TaxScheme.INC,
                    TaxMother.OTRA_EMPRESA, TaxMother.EMPLOYEE_ID, 2L);

            assertThat(tax.getName()).isEqualTo("IVA Reducido");
            assertThat(tax.getPercentage()).isEqualByComparingTo("5.00");
            assertThat(tax.getTaxScheme()).isEqualTo(TaxScheme.INC);
            assertThat(tax.getCompany()).isEqualTo(TaxMother.OTRA_EMPRESA);
            assertThat(tax.getUpdatedBy()).isEqualTo(TaxMother.EMPLOYEE_ID);
            assertThat(tax.getVersion()).isEqualTo(2L);
            assertThat(tax.getUpdatedDate()).isNotNull();
        }

        @Test
        @DisplayName("update tambien valida los invariantes del impuesto")
        void update_tambien_valida_los_invariantes() {
            Tax tax = TaxMother.activo();

            assertThatThrownBy(() -> tax.update("", new BigDecimal("5.00"), TaxScheme.INC,
                    TaxMother.EMPRESA, TaxMother.EMPLOYEE_ID, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("Habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("enable marca el impuesto como activo")
        void enable_marca_el_impuesto_como_activo() {
            Tax tax = TaxMother.inactivo();

            tax.enable();

            assertThat(tax.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("disable marca el impuesto como inactivo")
        void disable_marca_el_impuesto_como_inactivo() {
            Tax tax = TaxMother.activo();

            tax.disable();

            assertThat(tax.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        private static Tax construir(String name, BigDecimal percentage, TaxScheme taxScheme,
                CompanyRef company) {
            return new Tax(null, name, percentage, taxScheme, company, LocalDateTime.now(), null,
                    null, null, true);
        }

        @Test
        @DisplayName("el nombre nulo no es valido")
        void el_nombre_nulo_no_es_valido() {
            assertThatThrownBy(() -> construir(null, new BigDecimal("19.00"), TaxScheme.IVA,
                    TaxMother.EMPRESA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("el nombre en blanco no es valido")
        void el_nombre_en_blanco_no_es_valido() {
            assertThatThrownBy(() -> construir("   ", new BigDecimal("19.00"), TaxScheme.IVA,
                    TaxMother.EMPRESA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("el nombre de mas de 100 caracteres no es valido")
        void el_nombre_de_mas_de_100_caracteres_no_es_valido() {
            String nombreLargo = "a".repeat(101);

            assertThatThrownBy(() -> construir(nombreLargo, new BigDecimal("19.00"), TaxScheme.IVA,
                    TaxMother.EMPRESA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name must be 100 chars or less");
        }

        @Test
        @DisplayName("el porcentaje nulo no es valido")
        void el_porcentaje_nulo_no_es_valido() {
            assertThatThrownBy(
                    () -> construir("IVA General", null, TaxScheme.IVA, TaxMother.EMPRESA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("percentage is required");
        }

        @Test
        @DisplayName("el porcentaje negativo no es valido")
        void el_porcentaje_negativo_no_es_valido() {
            assertThatThrownBy(() -> construir("IVA General", new BigDecimal("-0.01"),
                    TaxScheme.IVA, TaxMother.EMPRESA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("percentage cannot be negative");
        }

        @Test
        @DisplayName("el porcentaje mayor a 100 no es valido")
        void el_porcentaje_mayor_a_100_no_es_valido() {
            assertThatThrownBy(() -> construir("IVA General", new BigDecimal("100.01"),
                    TaxScheme.IVA, TaxMother.EMPRESA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("percentage cannot exceed 100");
        }

        @Test
        @DisplayName("el esquema nulo no es valido")
        void el_esquema_nulo_no_es_valido() {
            assertThatThrownBy(() -> construir("IVA General", new BigDecimal("19.00"), null,
                    TaxMother.EMPRESA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxScheme is required");
        }

        @Test
        @DisplayName("la empresa nula no es valida")
        void la_empresa_nula_no_es_valida() {
            assertThatThrownBy(
                    () -> construir("IVA General", new BigDecimal("19.00"), TaxScheme.IVA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @Test
        @DisplayName("un porcentaje en los limites 0 y 100 es valido")
        void un_porcentaje_en_los_limites_es_valido() {
            assertThat(construir("Exento", BigDecimal.ZERO, TaxScheme.IVA, TaxMother.EMPRESA)
                    .getPercentage()).isEqualByComparingTo("0");
            assertThat(
                    construir("Maximo", new BigDecimal("100.00"), TaxScheme.IVA, TaxMother.EMPRESA)
                            .getPercentage())
                    .isEqualByComparingTo("100.00");
        }
    }
}
