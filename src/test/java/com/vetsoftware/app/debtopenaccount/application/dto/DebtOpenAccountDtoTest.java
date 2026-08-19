package com.vetsoftware.app.debtopenaccount.application.dto;

import com.vetsoftware.app.shared.pagination.PageResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de debtopenaccount")
class DebtOpenAccountDtoTest {

    @Nested
    @DisplayName("DebtOpenAccountDto.from")
    class Proyeccion {

        @Test
        @DisplayName("traslada cada campo del abono, uno por uno")
        void traslada_cada_campo_del_abono() {
            DebtOpenAccountDto dto = DebtOpenAccountDto.from(DebtOpenAccountMother.abono());

            assertThat(dto.id()).isEqualTo(DebtOpenAccountMother.PAYMENT_ID);
            assertThat(dto.amount()).isEqualByComparingTo("30000");
            assertThat(dto.paymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(dto.openAccount()).isEqualTo(new OpenAccountSummaryDto(
                    DebtOpenAccountMother.OPEN_ACCOUNT_ID, DebtOpenAccountMother.COMPANY_ID));
            assertThat(dto.createdBy()).isEqualTo(new EmployeeSummaryDto(7L, "Ana Ruiz"));
            assertThat(dto.createdDate()).isEqualTo(DebtOpenAccountMother.CREADO);
            assertThat(dto.enabled()).isTrue();
            assertThat(dto.voided()).isFalse();
            assertThat(dto.voidedBy()).isNull();
            assertThat(dto.voidedAt()).isNull();
            assertThat(dto.voidReason()).isNull();
        }

        @Test
        @DisplayName("proyecta el rastro de anulacion cuando el abono esta anulado")
        void proyecta_el_rastro_de_anulacion() {
            DebtOpenAccountDto dto = DebtOpenAccountDto.from(DebtOpenAccountMother.abonoAnulado());

            assertThat(dto.voided()).isTrue();
            assertThat(dto.voidedBy()).isEqualTo(new EmployeeSummaryDto(8L, "Luis Paz"));
            assertThat(dto.voidedAt()).isEqualTo(DebtOpenAccountMother.ANULADO);
            assertThat(dto.voidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("un abono sin creador no revienta: el resumen queda null")
        void un_abono_sin_creador_no_revienta() {
            DebtOpenAccount sinCreador = new DebtOpenAccount(1L, DebtOpenAccountMother.MONTO,
                    PaymentMethod.CASH, DebtOpenAccountMother.CUENTA, null,
                    DebtOpenAccountMother.CREADO, null, true, false, null, null, null, null);

            DebtOpenAccountDto dto = DebtOpenAccountDto.from(sinCreador);

            assertThat(dto.createdBy()).isNull();
            assertThat(dto.voidedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("resumenes companion")
    class Resumenes {

        @Test
        @DisplayName("cada resumen copia solo los campos que expone la API")
        void cada_resumen_copia_solo_lo_que_expone_la_api() {
            assertThat(EmployeeSummaryDto.from(DebtOpenAccountMother.EMPLEADO))
                    .isEqualTo(new EmployeeSummaryDto(7L, "Ana Ruiz"));
            assertThat(OpenAccountSummaryDto.from(DebtOpenAccountMother.CUENTA))
                    .isEqualTo(new OpenAccountSummaryDto(50L, 9L));
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
