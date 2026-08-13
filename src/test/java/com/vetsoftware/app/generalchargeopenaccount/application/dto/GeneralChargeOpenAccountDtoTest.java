package com.vetsoftware.app.generalchargeopenaccount.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de generalchargeopenaccount")
class GeneralChargeOpenAccountDtoTest {

    @Nested
    @DisplayName("GeneralChargeOpenAccountDto.from")
    class Proyeccion {

        @Test
        @DisplayName("traslada cada campo del agregado, uno por uno")
        void traslada_cada_campo_del_agregado() {
            GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto
                    .from(GeneralChargeOpenAccountMother.cargo());

            assertThat(dto.id()).isEqualTo(GeneralChargeOpenAccountMother.CHARGE_ID);
            assertThat(dto.name()).isEqualTo(GeneralChargeOpenAccountMother.NOMBRE);
            assertThat(dto.unitAmount()).isEqualByComparingTo("5950");
            assertThat(dto.quantity()).isEqualByComparingTo("2");
            assertThat(dto.tax())
                    .isEqualTo(new TaxSummaryDto(4L, "IVA 19%", new BigDecimal("19.00")));
            assertThat(dto.hasTax()).isTrue();
            assertThat(dto.taxPercentage()).isEqualByComparingTo("19.00");
            assertThat(dto.taxName()).isEqualTo("IVA 19%");
            assertThat(dto.baseAmount()).isEqualByComparingTo("10000.00");
            assertThat(dto.taxAmount()).isEqualByComparingTo("1900.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("11900.00");
            assertThat(dto.openAccount()).isEqualTo(
                    new OpenAccountSummaryDto(GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                            GeneralChargeOpenAccountMother.COMPANY_ID));
            assertThat(dto.createdBy()).isEqualTo(new EmployeeSummaryDto(7L, "Ana Ruiz"));
            assertThat(dto.createdDate()).isEqualTo(GeneralChargeOpenAccountMother.CREADO);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.voided()).isFalse();
            assertThat(dto.voidedBy()).isNull();
            assertThat(dto.voidedAt()).isNull();
            assertThat(dto.voidReason()).isNull();
        }

        @Test
        @DisplayName("proyecta el rastro de anulacion cuando el cargo esta anulado")
        void proyecta_el_rastro_de_anulacion() {
            GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto
                    .from(GeneralChargeOpenAccountMother.cargoAnulado());

            assertThat(dto.voided()).isTrue();
            assertThat(dto.voidedBy()).isEqualTo(new EmployeeSummaryDto(8L, "Luis Paz"));
            assertThat(dto.voidedAt()).isEqualTo(GeneralChargeOpenAccountMother.ANULADO);
            assertThat(dto.voidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("un cargo sin impuesto proyecta el resumen en null y el impuesto en cero")
        void un_cargo_sin_impuesto_proyecta_null_y_cero() {
            GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto
                    .from(GeneralChargeOpenAccountMother.cargoSinImpuesto());

            assertThat(dto.tax()).isNull();
            assertThat(dto.hasTax()).isFalse();
            assertThat(dto.taxPercentage()).isNull();
            assertThat(dto.taxName()).isNull();
            assertThat(dto.taxAmount()).isEqualByComparingTo("0.00");
            assertThat(dto.baseAmount()).isEqualByComparingTo("11900.00");
        }
    }

    @Nested
    @DisplayName("resumenes companion")
    class Resumenes {

        @Test
        @DisplayName("cada resumen copia solo los campos que expone la API")
        void cada_resumen_copia_solo_lo_que_expone_la_api() {
            assertThat(EmployeeSummaryDto.from(GeneralChargeOpenAccountMother.EMPLEADO))
                    .isEqualTo(new EmployeeSummaryDto(7L, "Ana Ruiz"));
            assertThat(OpenAccountSummaryDto.from(GeneralChargeOpenAccountMother.CUENTA))
                    .isEqualTo(new OpenAccountSummaryDto(50L, 9L));

            TaxSummaryDto impuesto = TaxSummaryDto.from(GeneralChargeOpenAccountMother.IVA_19);
            assertThat(impuesto.id()).isEqualTo(4L);
            assertThat(impuesto.name()).isEqualTo("IVA 19%");
            assertThat(impuesto.percentage()).isEqualByComparingTo("19.00");
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Pagina {

        @Test
        @DisplayName("map transforma el contenido y conserva los metadatos de la pagina")
        void map_transforma_el_contenido_y_conserva_los_metadatos() {
            PageResult<Integer> pagina = new PageResult<>(List.of(1, 2, 3), 2, 20, 41L, 3);

            PageResult<String> mapeada = pagina.map(String::valueOf);

            assertThat(mapeada.content()).containsExactly("1", "2", "3");
            assertThat(mapeada.page()).isEqualTo(2);
            assertThat(mapeada.pageSize()).isEqualTo(20);
            assertThat(mapeada.totalElements()).isEqualTo(41L);
            assertThat(mapeada.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("una pagina vacia se mapea a una pagina vacia, no a null")
        void una_pagina_vacia_se_mapea_a_vacia() {
            PageResult<Integer> vacia = new PageResult<>(List.of(), 0, 20, 0L, 0);

            assertThat(vacia.map(Function.identity()).content()).isEmpty();
        }
    }
}
