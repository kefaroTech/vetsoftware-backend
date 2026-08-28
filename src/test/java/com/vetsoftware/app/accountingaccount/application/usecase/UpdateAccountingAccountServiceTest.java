package com.vetsoftware.app.accountingaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import com.vetsoftware.app.accountingaccount.testsupport.AccountingAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateAccountingAccountService")
class UpdateAccountingAccountServiceTest {

    @Mock
    private AccountingAccountRepository repository;

    @Captor
    private ArgumentCaptor<AccountingAccount> cuentaCaptor;

    @InjectMocks
    private UpdateAccountingAccountService service;

    @Nested
    @DisplayName("cuenta existente")
    class CuentaExistente {

        @Test
        @DisplayName("corrige nombre y tercero, sin tocar codigo, clase, nivel o padre")
        void corrige_nombre_y_tercero_sin_tocar_el_resto() {
            AccountingAccount original = AccountingAccountMother.cuentaPostable();
            when(repository.findById(AccountingAccountMother.ACCOUNT_ID))
                    .thenReturn(Optional.of(original));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AccountingAccountDto dto = service.execute(
                    AccountingAccountMother.comandoActualizar(AccountingAccountMother.ACCOUNT_ID));

            verify(repository).save(cuentaCaptor.capture());
            AccountingAccount guardada = cuentaCaptor.getValue();
            assertThat(guardada.getName()).isEqualTo("Caja general - sede norte");
            assertThat(guardada.isRequiresThirdParty()).isTrue();
            assertThat(guardada.getCode()).isEqualTo(original.getCode());
            assertThat(guardada.getAccountClass()).isEqualTo(original.getAccountClass());
            assertThat(guardada.getAccountLevel()).isEqualTo(original.getAccountLevel());
            assertThat(guardada.getParentCode()).isEqualTo(original.getParentCode());
            assertThat(guardada.getVersion()).isEqualTo(original.getVersion());
            assertThat(dto.name()).isEqualTo("Caja general - sede norte");
        }
    }

    @Nested
    @DisplayName("cuenta inexistente")
    class CuentaInexistente {

        @Test
        @DisplayName("lanza y no guarda")
        void lanza_y_no_guarda() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(AccountingAccountMother.comandoActualizar(999L)))
                    .isInstanceOf(AccountingAccountNotFoundException.class)
                    .hasMessageContaining("Accounting account not found: 999");

            verify(repository, never()).save(any());
        }
    }
}
