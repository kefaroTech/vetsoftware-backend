package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.shared.pagination.PageResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.productchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.productchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de productchargeopenaccount")
class ProductChargeOpenAccountDtoTest {

    @Nested
    @DisplayName("ProductChargeOpenAccountDto.from")
    class Principal {

        @Test
        @DisplayName("copia campo por campo el cargo activo")
        void copia_campo_por_campo_el_cargo_activo() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccountMother.cargo();

            ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(charge);

            assertThat(dto.id()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            assertThat(dto.animal().id()).isEqualTo(ProductChargeOpenAccountMother.ANIMAL.id());
            assertThat(dto.animal().name()).isEqualTo("Firulais");
            assertThat(dto.animal().code()).isEqualTo("A-001");
            assertThat(dto.product().id()).isEqualTo(ProductChargeOpenAccountMother.PRODUCTO.id());
            assertThat(dto.product().name()).isEqualTo("Alimento");
            assertThat(dto.product().code()).isEqualTo("P-001");
            assertThat(dto.product().salePrice()).isEqualByComparingTo("11900");
            assertThat(dto.unitPrice()).isEqualByComparingTo("11900");
            assertThat(dto.quantity()).isEqualTo(1);
            assertThat(dto.hasTax()).isTrue();
            assertThat(dto.taxPercentage()).isEqualByComparingTo("19.00");
            assertThat(dto.taxName()).isEqualTo("IVA 19%");
            assertThat(dto.baseAmount()).isEqualByComparingTo("10000.00");
            assertThat(dto.taxAmount()).isEqualByComparingTo("1900.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("11900.00");
            assertThat(dto.openAccount().id())
                    .isEqualTo(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
            assertThat(dto.openAccount().companyId())
                    .isEqualTo(ProductChargeOpenAccountMother.COMPANY_ID);
            assertThat(dto.createdBy().id())
                    .isEqualTo(ProductChargeOpenAccountMother.EMPLEADO.id());
            assertThat(dto.createdBy().name()).isEqualTo("Ana Ruiz");
            assertThat(dto.createdDate()).isEqualTo(ProductChargeOpenAccountMother.CREADO);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.voided()).isFalse();
            assertThat(dto.voidedBy()).isNull();
            assertThat(dto.voidedAt()).isNull();
            assertThat(dto.voidReason()).isNull();
        }

        @Test
        @DisplayName("copia la traza de anulacion cuando el cargo esta anulado")
        void copia_la_traza_de_anulacion() {
            ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto
                    .from(ProductChargeOpenAccountMother.cargoAnulado());

            assertThat(dto.voided()).isTrue();
            assertThat(dto.voidedBy().id())
                    .isEqualTo(ProductChargeOpenAccountMother.OTRO_EMPLEADO.id());
            assertThat(dto.voidedBy().name()).isEqualTo("Luis Paz");
            assertThat(dto.voidedAt()).isEqualTo(ProductChargeOpenAccountMother.ANULADO);
            assertThat(dto.voidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("tolera un cargo sin creador (dato legacy) sin reventar")
        void tolera_un_cargo_sin_creador() {
            ProductChargeOpenAccount sinCreador = new ProductChargeOpenAccount(1L,
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    new BigDecimal("100"), ProductChargeOpenAccountMother.CUENTA, null,
                    ProductChargeOpenAccountMother.CREADO, true);

            ProductChargeOpenAccountDto dto = ProductChargeOpenAccountDto.from(sinCreador);

            assertThat(dto.createdBy()).isNull();
            assertThat(dto.voidedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("companions")
    class Companions {

        @Test
        @DisplayName("AnimalSummaryDto copia los tres campos")
        void animal_summary() {
            AnimalSummaryDto dto = AnimalSummaryDto.from(new AnimalRef(1L, "Firulais", "A-001"));

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Firulais");
            assertThat(dto.code()).isEqualTo("A-001");
        }

        @Test
        @DisplayName("ProductSummaryDto copia los cuatro campos")
        void product_summary() {
            ProductSummaryDto dto = ProductSummaryDto
                    .from(new ProductRef(2L, "Alimento", "P-001", new BigDecimal("11900")));

            assertThat(dto.id()).isEqualTo(2L);
            assertThat(dto.name()).isEqualTo("Alimento");
            assertThat(dto.code()).isEqualTo("P-001");
            assertThat(dto.salePrice()).isEqualByComparingTo("11900");
        }

        @Test
        @DisplayName("OpenAccountSummaryDto copia id y companyId")
        void open_account_summary() {
            OpenAccountSummaryDto dto = OpenAccountSummaryDto.from(new OpenAccountRef(50L, 9L));

            assertThat(dto.id()).isEqualTo(50L);
            assertThat(dto.companyId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("EmployeeSummaryDto copia id y nombre")
        void employee_summary() {
            EmployeeSummaryDto dto = EmployeeSummaryDto.from(new EmployeeRef(7L, "Ana Ruiz"));

            assertThat(dto.id()).isEqualTo(7L);
            assertThat(dto.name()).isEqualTo("Ana Ruiz");
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Pagina {

        @Test
        @DisplayName("map transforma el contenido y conserva los metadatos de paginacion")
        void map_transforma_el_contenido_y_conserva_los_metadatos() {
            PageResult<ProductChargeOpenAccount> pagina = new PageResult<>(
                    List.of(ProductChargeOpenAccountMother.cargo()), 2, 20, 41L, 3);

            PageResult<ProductChargeOpenAccountDto> mapeada = pagina
                    .map(ProductChargeOpenAccountDto::from);

            assertThat(mapeada.content()).singleElement()
                    .extracting(ProductChargeOpenAccountDto::id)
                    .isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            assertThat(mapeada.page()).isEqualTo(2);
            assertThat(mapeada.pageSize()).isEqualTo(20);
            assertThat(mapeada.totalElements()).isEqualTo(41L);
            assertThat(mapeada.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("map sobre una pagina vacia devuelve una pagina vacia")
        void map_sobre_una_pagina_vacia() {
            PageResult<ProductChargeOpenAccount> pagina = new PageResult<>(List.of(), 0, 20, 0L, 0);

            assertThat(pagina.map(ProductChargeOpenAccountDto::from).content()).isEmpty();
        }
    }
}
