package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.shared.pagination.PageResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de servicechargeopenaccount")
class ServiceChargeOpenAccountDtoTest {

    @Nested
    @DisplayName("ServiceChargeOpenAccountDto.from")
    class Proyeccion {

        @Test
        @DisplayName("traslada cada campo del agregado, uno por uno")
        void traslada_cada_campo_del_agregado() {
            ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto
                    .from(ServiceChargeOpenAccountMother.cargo());

            assertThat(dto.id()).isEqualTo(ServiceChargeOpenAccountMother.CHARGE_ID);
            assertThat(dto.animal()).isEqualTo(new AnimalSummaryDto(1L, "Firulais", "A-001"));
            assertThat(dto.service().id()).isEqualTo(2L);
            assertThat(dto.service().name()).isEqualTo("Consulta general");
            assertThat(dto.service().price()).isEqualByComparingTo("11900");
            assertThat(dto.unitPrice()).isEqualByComparingTo("11900");
            assertThat(dto.hasTax()).isTrue();
            assertThat(dto.taxPercentage()).isEqualByComparingTo("19.00");
            assertThat(dto.taxName()).isEqualTo("IVA 19%");
            assertThat(dto.baseAmount()).isEqualByComparingTo("10000.00");
            assertThat(dto.taxAmount()).isEqualByComparingTo("1900.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("11900.00");
            assertThat(dto.openAccount()).isEqualTo(
                    new OpenAccountSummaryDto(ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID,
                            ServiceChargeOpenAccountMother.COMPANY_ID));
            assertThat(dto.createdBy()).isEqualTo(new EmployeeSummaryDto(7L, "Ana Ruiz"));
            assertThat(dto.createdDate()).isEqualTo(ServiceChargeOpenAccountMother.CREADO);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.voided()).isFalse();
            assertThat(dto.voidedBy()).isNull();
            assertThat(dto.voidedAt()).isNull();
            assertThat(dto.voidReason()).isNull();
        }

        @Test
        @DisplayName("proyecta el rastro de anulacion cuando el cargo esta anulado")
        void proyecta_el_rastro_de_anulacion() {
            ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto
                    .from(ServiceChargeOpenAccountMother.cargoAnulado());

            assertThat(dto.voided()).isTrue();
            assertThat(dto.voidedBy()).isEqualTo(new EmployeeSummaryDto(8L, "Luis Paz"));
            assertThat(dto.voidedAt()).isEqualTo(ServiceChargeOpenAccountMother.ANULADO);
            assertThat(dto.voidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("un cargo sin creador no revienta: el resumen queda null")
        void un_cargo_sin_creador_no_revienta() {
            ServiceChargeOpenAccount sinCreador = new ServiceChargeOpenAccount(1L,
                    ServiceChargeOpenAccountMother.ANIMAL, ServiceChargeOpenAccountMother.SERVICIO,
                    ServiceChargeOpenAccountMother.PRECIO, ServiceChargeOpenAccountMother.CUENTA,
                    null, ServiceChargeOpenAccountMother.CREADO, true);

            ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto.from(sinCreador);

            assertThat(dto.createdBy()).isNull();
            assertThat(dto.voidedBy()).isNull();
        }

        @Test
        @DisplayName("un cargo sin impuesto proyecta el desglose en cero, no en null")
        void un_cargo_sin_impuesto_proyecta_cero() {
            ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto
                    .from(ServiceChargeOpenAccountMother.cargoSinImpuesto());

            assertThat(dto.hasTax()).isFalse();
            assertThat(dto.taxPercentage()).isNull();
            assertThat(dto.taxName()).isNull();
            assertThat(dto.taxAmount()).isEqualByComparingTo("0.00");
            assertThat(dto.baseAmount()).isEqualByComparingTo("5000.00");
        }
    }

    @Nested
    @DisplayName("resumenes companion")
    class Resumenes {

        @Test
        @DisplayName("cada resumen copia solo los campos que expone la API")
        void cada_resumen_copia_solo_lo_que_expone_la_api() {
            assertThat(AnimalSummaryDto.from(ServiceChargeOpenAccountMother.ANIMAL))
                    .isEqualTo(new AnimalSummaryDto(1L, "Firulais", "A-001"));
            assertThat(EmployeeSummaryDto.from(ServiceChargeOpenAccountMother.EMPLEADO))
                    .isEqualTo(new EmployeeSummaryDto(7L, "Ana Ruiz"));
            assertThat(OpenAccountSummaryDto.from(ServiceChargeOpenAccountMother.CUENTA))
                    .isEqualTo(new OpenAccountSummaryDto(50L, 9L));

            ServiceSummaryDto servicio = ServiceSummaryDto
                    .from(ServiceChargeOpenAccountMother.SERVICIO);
            assertThat(servicio.id()).isEqualTo(2L);
            assertThat(servicio.name()).isEqualTo("Consulta general");
            assertThat(servicio.price()).isEqualByComparingTo("11900");
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
