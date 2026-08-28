package com.vetsoftware.app.accountingperiod.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountingPeriodDto — la proyeccion del mes contable")
class AccountingPeriodDtoTest {

    private static final Long ID = 8800L;

    @Test
    @DisplayName("copia los nueve campos del periodo reabierto sin cruzar ninguno")
    void copia_los_nueve_campos_sin_cruzarlos() {
        // Se proyecta el periodo REABIERTO porque es el unico que tiene los cinco
        // campos de cierre y reapertura llenos a la vez: sobre un mes abierto,
        // cruzar closedAt con reopenedAt no se veria.
        AccountingPeriodDto dto = AccountingPeriodDto.from(AccountingPeriodMother.reabierto(ID));

        assertThat(dto.id()).isEqualTo(ID);
        assertThat(dto.periodKey()).isEqualTo("2026-03");
        assertThat(dto.status()).isEqualTo(AccountingPeriodStatus.OPEN);
        assertThat(dto.closedAt()).isEqualTo(AccountingPeriodMother.CERRADO_EL);
        assertThat(dto.closedBySystemUserId()).isEqualTo(AccountingPeriodMother.CERRADO_POR);
        assertThat(dto.reopenedAt()).isEqualTo(AccountingPeriodMother.REABIERTO_EL);
        assertThat(dto.reopenedBySystemUserId()).isEqualTo(AccountingPeriodMother.REABIERTO_POR);
        assertThat(dto.reopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
        assertThat(dto.createdDate()).isEqualTo(AccountingPeriodMother.CREADO_EL);
    }

    @Test
    @DisplayName("la clave sale como cadena plana, no como el value object")
    void la_clave_sale_como_cadena_plana() {
        // Publicar el VO haria que springdoc generara {"periodKey": {"value": "..."}}
        // y los dos fronts tendrian que desenvolverlo en cada pantalla.
        assertThat(AccountingPeriodDto.from(AccountingPeriodMother.abierto()).periodKey())
                .isEqualTo("2026-03");
    }

    @Test
    @DisplayName("un mes abierto proyecta nulos en los cinco campos de cierre y reapertura")
    void un_mes_abierto_proyecta_nulos() {
        AccountingPeriodDto dto = AccountingPeriodDto
                .from(AccountingPeriodMother.persistidoAbierto(ID));

        assertThat(dto.closedAt()).isNull();
        assertThat(dto.closedBySystemUserId()).isNull();
        assertThat(dto.reopenedAt()).isNull();
        assertThat(dto.reopenedBySystemUserId()).isNull();
        assertThat(dto.reopenedReason()).isNull();
    }
}
