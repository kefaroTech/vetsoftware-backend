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
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountAlreadyClosedException;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccountNotFoundException;
import com.vetsoftware.app.accountingaccount.testsupport.AccountingAccountMother;
import java.time.LocalDate;
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
@DisplayName("CloseAccountingAccountService")
class CloseAccountingAccountServiceTest {

    @Mock
    private AccountingAccountRepository repository;

    @Captor
    private ArgumentCaptor<AccountingAccount> cuentaCaptor;

    @InjectMocks
    private CloseAccountingAccountService service;

    @Nested
    @DisplayName("cuenta abierta")
    class CuentaAbierta {

        @Test
        @DisplayName("guarda la cuenta con la fecha de fin del comando, conservando la version")
        void guarda_con_la_fecha_de_fin_del_comando() {
            AccountingAccount abierta = AccountingAccountMother.cuentaPostable();
            when(repository.findById(AccountingAccountMother.ACCOUNT_ID))
                    .thenReturn(Optional.of(abierta));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AccountingAccountDto dto = service.execute(AccountingAccountMother
                    .comandoCerrar(AccountingAccountMother.ACCOUNT_ID, LocalDate.of(2026, 6, 1)));

            verify(repository).save(cuentaCaptor.capture());
            assertThat(cuentaCaptor.getValue().getValidTo()).isEqualTo(LocalDate.of(2026, 6, 1));
            // La version viaja intacta en el objeto que se guarda: es la barandilla del
            // ciclo leer-modificar-guardar con bloqueo optimista.
            assertThat(cuentaCaptor.getValue().getVersion()).isEqualTo(abierta.getVersion());
            assertThat(dto.validTo()).isEqualTo(LocalDate.of(2026, 6, 1));
        }
    }

    @Nested
    @DisplayName("cuentas que no admiten el cierre")
    class NoAdmitenCierre {

        @Test
        @DisplayName("cuenta inexistente: lanza y no guarda")
        void cuenta_inexistente() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(AccountingAccountMother.comandoCerrar(999L, LocalDate.of(2026, 6, 1))))
                    .isInstanceOf(AccountingAccountNotFoundException.class)
                    .hasMessageContaining("Accounting account not found: 999");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("cuenta ya cerrada: el dominio lo rechaza y el service no llega a guardar")
        void cuenta_ya_cerrada() {
            AccountingAccount cerrada = AccountingAccountMother
                    .cuentaCerrada(LocalDate.of(2026, 3, 1));
            when(repository.findById(AccountingAccountMother.ACCOUNT_ID))
                    .thenReturn(Optional.of(cerrada));

            assertThatThrownBy(() -> service.execute(AccountingAccountMother
                    .comandoCerrar(AccountingAccountMother.ACCOUNT_ID, LocalDate.of(2026, 6, 1))))
                    .isInstanceOf(AccountingAccountAlreadyClosedException.class)
                    .hasMessageContaining("already closed since 2026-03-01");

            verify(repository, never()).save(any());
        }
    }
}
