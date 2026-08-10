package com.vetsoftware.app.productchargeopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Companion VOs de productchargeopenaccount")
class ProductChargeOpenAccountRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class Animal {

        @Test
        @DisplayName("exige el id del animal")
        void exige_el_id() {
            assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal id is required");
        }

        @Test
        @DisplayName("nombre y codigo son opcionales")
        void nombre_y_codigo_son_opcionales() {
            assertThatCode(() -> new AnimalRef(1L, null, null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("conserva los tres campos")
        void conserva_los_tres_campos() {
            AnimalRef ref = new AnimalRef(1L, "Firulais", "A-001");

            assertThat(ref.id()).isEqualTo(1L);
            assertThat(ref.name()).isEqualTo("Firulais");
            assertThat(ref.code()).isEqualTo("A-001");
        }
    }

    @Nested
    @DisplayName("EmployeeRef")
    class Employee {

        @Test
        @DisplayName("exige el id del empleado")
        void exige_el_id() {
            assertThatThrownBy(() -> new EmployeeRef(null, "Ana"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee id is required");
        }

        @Test
        @DisplayName("el nombre es opcional")
        void el_nombre_es_opcional() {
            assertThatCode(() -> new EmployeeRef(7L, null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("OpenAccountRef")
    class OpenAccount {

        @Test
        @DisplayName("exige el id de la cuenta")
        void exige_el_id() {
            assertThatThrownBy(() -> new OpenAccountRef(null, 9L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("openAccount id is required");
        }

        @Test
        @DisplayName("conserva el companyId que usa el guard multi-tenant")
        void conserva_el_company_id() {
            OpenAccountRef ref = new OpenAccountRef(50L, 9L);

            assertThat(ref.id()).isEqualTo(50L);
            assertThat(ref.companyId()).isEqualTo(9L);
        }
    }

    @Nested
    @DisplayName("ProductRef")
    class Product {

        @Test
        @DisplayName("exige el id del producto")
        void exige_el_id() {
            assertThatThrownBy(() -> new ProductRef(null, "Alimento", "P-001", BigDecimal.TEN, true,
                    null, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("product id is required");
        }

        @Test
        @DisplayName("el constructor de compat deja el producto sin impuesto")
        void el_constructor_de_compat_deja_el_producto_sin_impuesto() {
            ProductRef ref = new ProductRef(2L, "Alimento", "P-001", new BigDecimal("11900"));

            assertThat(ref.hasTax()).isFalse();
            assertThat(ref.tax()).isNull();
            assertThat(ref.taxTreatment()).isNull();
            assertThat(ref.salePrice()).isEqualByComparingTo("11900");
        }

        @Test
        @DisplayName("admite producto sin precio: el cargo lo interpreta como cero")
        void admite_producto_sin_precio() {
            assertThatCode(() -> new ProductRef(2L, "Muestra", "P-005", null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("TaxRef")
    class Tax {

        @Test
        @DisplayName("exige el id del impuesto")
        void exige_el_id() {
            assertThatThrownBy(() -> new TaxRef(null, "IVA 19%", new BigDecimal("19")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax id is required");
        }

        @ParameterizedTest(name = "nombre [{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige el nombre del impuesto")
        void exige_el_nombre(String nombre) {
            assertThatThrownBy(() -> new TaxRef(4L, nombre, new BigDecimal("19")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax name is required");
        }

        @Test
        @DisplayName("exige el porcentaje del impuesto")
        void exige_el_porcentaje() {
            assertThatThrownBy(() -> new TaxRef(4L, "IVA 19%", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax percentage is required");
        }

        @Test
        @DisplayName("el constructor de compat deja el esquema en null")
        void el_constructor_de_compat_deja_el_esquema_en_null() {
            TaxRef ref = new TaxRef(4L, "IVA 19%", new BigDecimal("19.00"));

            assertThat(ref.scheme()).isNull();
            assertThat(ref.percentage()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("acepta el 0 % que distingue exento de excluido")
        void acepta_el_cero_por_ciento() {
            TaxRef ref = new TaxRef(5L, "IVA 0%", BigDecimal.ZERO, "IVA");

            assertThat(ref.percentage()).isEqualByComparingTo("0");
            assertThat(ref.scheme()).isEqualTo("IVA");
        }
    }
}
