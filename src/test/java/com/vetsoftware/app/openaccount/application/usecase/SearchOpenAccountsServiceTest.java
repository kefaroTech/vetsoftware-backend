package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchOpenAccountsService")
class SearchOpenAccountsServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private SearchOpenAccountsService service;

    @Test
    @DisplayName("delega el command en el repositorio y mapea la pagina a dto")
    void delega_el_command_y_mapea_la_pagina() {
        OpenAccount cuenta = OpenAccountMother.abierta();
        SearchOpenAccountsCommand command = new SearchOpenAccountsCommand(
                OpenAccountMother.COMPANY_ID, OpenAccountMother.OWNER_ID, true,
                List.of(OpenAccountStatus.OPEN), "juan", 0, 20, OpenAccountMother.BRANCH_ID);
        when(repository.search(command))
                .thenReturn(new PageResult<>(List.of(cuenta), 0, 20, 1L, 1));

        PageResult<OpenAccountDto> pagina = service.execute(command);

        assertThat(pagina.content()).extracting(OpenAccountDto::id)
                .containsExactly(OpenAccountMother.OPEN_ACCOUNT_ID);
    }
}
