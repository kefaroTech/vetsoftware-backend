package com.vetsoftware.app.accountingaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import com.vetsoftware.app.accountingaccount.testsupport.AccountingAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAccountingAccountService")
class FindAccountingAccountServiceTest {

    @Mock
    private AccountingAccountRepository repository;

    @InjectMocks
    private FindAccountingAccountService service;

    @Nested
    @DisplayName("por id")
    class PorId {

        @Test
        @DisplayName("devuelve el DTO cuando la cuenta existe")
        void devuelve_el_dto_cuando_existe() {
            when(repository.findById(AccountingAccountMother.ACCOUNT_ID))
                    .thenReturn(Optional.of(AccountingAccountMother.cuentaPostable()));

            AccountingAccountDto dto = service.findById(AccountingAccountMother.ACCOUNT_ID);

            assertThat(dto.code()).isEqualTo(AccountingAccountMother.CODE);
        }

        @Test
        @DisplayName("lanza cuando la cuenta no existe")
        void lanza_cuando_no_existe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(AccountingAccountNotFoundException.class)
                    .hasMessageContaining("Accounting account not found: 999");
        }
    }

    @Nested
    @DisplayName("por codigo")
    class PorCodigo {

        @Test
        @DisplayName("devuelve el DTO cuando el codigo existe")
        void devuelve_el_dto_cuando_el_codigo_existe() {
            when(repository.findByCode(AccountingAccountMother.CODE))
                    .thenReturn(Optional.of(AccountingAccountMother.cuentaPostable()));

            AccountingAccountDto dto = service.findByCode(AccountingAccountMother.CODE);

            assertThat(dto.id()).isEqualTo(AccountingAccountMother.ACCOUNT_ID);
        }

        @Test
        @DisplayName("lanza cuando el codigo no existe, con el codigo en el mensaje")
        void lanza_cuando_el_codigo_no_existe() {
            when(repository.findByCode("999999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByCode("999999"))
                    .isInstanceOf(AccountingAccountNotFoundException.class)
                    .hasMessageContaining("Accounting account not found: 999999");
        }
    }
}
