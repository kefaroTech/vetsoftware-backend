package com.vetsoftware.app.accountingaccount.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.testsupport.AccountingAccountMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AccountingAccountDto")
class AccountingAccountDtoTest {

    @Test
    @DisplayName("from copia cada campo del dominio, campo por campo")
    void from_copia_cada_campo() {
        AccountingAccount cuenta = AccountingAccountMother.cuentaPostable();

        AccountingAccountDto dto = AccountingAccountDto.from(cuenta);

        assertThat(dto.id()).isEqualTo(cuenta.getId());
        assertThat(dto.code()).isEqualTo(cuenta.getCode());
        assertThat(dto.name()).isEqualTo(cuenta.getName());
        assertThat(dto.accountClass()).isEqualTo(cuenta.getAccountClass());
        assertThat(dto.parentCode()).isEqualTo(cuenta.getParentCode());
        assertThat(dto.accountLevel()).isEqualTo(cuenta.getAccountLevel());
        assertThat(dto.postable()).isEqualTo(cuenta.isPostable());
        assertThat(dto.requiresThirdParty()).isEqualTo(cuenta.isRequiresThirdParty());
        assertThat(dto.validFrom()).isEqualTo(cuenta.getValidFrom());
        assertThat(dto.validTo()).isEqualTo(cuenta.getValidTo());
        assertThat(dto.createdDate()).isEqualTo(cuenta.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(cuenta.isEnabled());
    }

    @Test
    @DisplayName("from propaga la fecha de fin de una cuenta cerrada")
    void from_propaga_la_fecha_de_fin() {
        AccountingAccount cerrada = AccountingAccountMother
                .cuentaCerrada(java.time.LocalDate.of(2026, 6, 1));

        AccountingAccountDto dto = AccountingAccountDto.from(cerrada);

        assertThat(dto.validTo()).isEqualTo(java.time.LocalDate.of(2026, 6, 1));
    }
}
