package com.vetsoftware.app.accountingaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import com.vetsoftware.app.accountingaccount.application.port.out.AccountingAccountRepository;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.testsupport.AccountingAccountMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAccountingAccountsService")
class ListAccountingAccountsServiceTest {

    @Mock
    private AccountingAccountRepository repository;

    @InjectMocks
    private ListAccountingAccountsService service;

    @Test
    @DisplayName("traduce la pagina de dominio a una pagina de DTOs sin recalcular los totales")
    void traduce_la_pagina_de_dominio_a_dtos() {
        List<AccountingAccount> contenido = List.of(AccountingAccountMother.cuentaPostable());
        PageResult<AccountingAccount> pagina = PageResult.of(contenido, 0, 20, 37L);
        when(repository.findAllEnabled(0, 20)).thenReturn(pagina);

        PageResult<AccountingAccountDto> resultado = service.listAll(0, 20);

        verify(repository).findAllEnabled(0, 20);
        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).code()).isEqualTo(AccountingAccountMother.CODE);
        // El total sale de la consulta, no de contar el contenido de esta pagina: con
        // una sola fila en `content` y totalElements=37 se distingue de un total mal
        // recalculado sobre la pagina ya servida.
        assertThat(resultado.totalElements()).isEqualTo(37L);
    }
}
