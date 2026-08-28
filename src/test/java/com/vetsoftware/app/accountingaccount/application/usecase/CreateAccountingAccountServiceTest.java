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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El reloj se inyecta como {@code Clock.fixed}, nunca se mockea: es un valor,
 * no un colaborador con efectos, y mockearlo obligaria a stubear
 * {@code instant()}/{@code getZone()} sin ganar nada sobre usar el real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountingAccountService")
class CreateAccountingAccountServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private AccountingAccountRepository repository;

    @Captor
    private ArgumentCaptor<AccountingAccount> cuentaCaptor;

    private CreateAccountingAccountService service;

    @BeforeEach
    void setUp() {
        service = new CreateAccountingAccountService(repository, RELOJ);
    }

    @Nested
    @DisplayName("cuenta raiz")
    class CuentaRaiz {

        @Test
        @DisplayName("una raiz sin padre no consulta si el padre existe")
        void raiz_no_consulta_padre() {
            when(repository.save(any())).thenReturn(AccountingAccountMother.cuentaRaiz());

            service.execute(AccountingAccountMother.comandoCrearRaiz());

            verify(repository, never()).existsByCode(any());
        }
    }

    @Nested
    @DisplayName("cuenta con padre")
    class CuentaConPadre {

        @Test
        @DisplayName("con el padre existente, persiste la cuenta con la fecha del reloj inyectado")
        void con_padre_existente_persiste() {
            when(repository.existsByCode(AccountingAccountMother.PARENT_CODE)).thenReturn(true);
            when(repository.save(any())).thenReturn(AccountingAccountMother.cuentaPostable());

            AccountingAccountDto dto = service.execute(AccountingAccountMother.comandoCrearHija());

            verify(repository).save(cuentaCaptor.capture());
            AccountingAccount guardada = cuentaCaptor.getValue();
            assertThat(guardada.getCode()).isEqualTo(AccountingAccountMother.CODE);
            assertThat(guardada.getParentCode()).isEqualTo(AccountingAccountMother.PARENT_CODE);
            assertThat(guardada.getCreatedDate()).isEqualTo(LocalDateTime.now(RELOJ));
            assertThat(guardada.getId()).isNull();
            assertThat(dto.id()).isEqualTo(AccountingAccountMother.ACCOUNT_ID);
        }

        @Test
        @DisplayName("con el padre inexistente, lanza y no guarda nada")
        void con_padre_inexistente_no_guarda() {
            when(repository.existsByCode(AccountingAccountMother.PARENT_CODE)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(AccountingAccountMother.comandoCrearHija()))
                    .isInstanceOf(AccountingAccountNotFoundException.class).hasMessageContaining(
                            "Accounting account not found: " + AccountingAccountMother.PARENT_CODE);

            verify(repository, never()).save(any());
        }
    }
}
