package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
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
@DisplayName("ListOpenAccountsService")
class ListOpenAccountsServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private ListOpenAccountsService service;

    @Test
    @DisplayName("delega en el repositorio scoped a empresa y sede, y mapea la pagina a dto")
    void delega_en_el_repositorio_y_mapea_la_pagina() {
        OpenAccount cuenta = OpenAccountMother.abierta();
        when(repository.findAllByCompanyId(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.BRANCH_ID, 0, 20))
                .thenReturn(new PageResult<>(List.of(cuenta), 0, 20, 1L, 1));

        PageResult<OpenAccountDto> pagina = service.listByCompany(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.BRANCH_ID, 0, 20);

        assertThat(pagina.content()).extracting(OpenAccountDto::id)
                .containsExactly(OpenAccountMother.OPEN_ACCOUNT_ID);
        assertThat(pagina.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("una pagina vacia se mapea a una pagina vacia, no null")
    void pagina_vacia_se_mapea_a_pagina_vacia() {
        when(repository.findAllByCompanyId(OpenAccountMother.COMPANY_ID, null, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<OpenAccountDto> pagina = service.listByCompany(OpenAccountMother.COMPANY_ID,
                null, 0, 20);

        assertThat(pagina.content()).isEmpty();
        assertThat(pagina.totalElements()).isZero();
    }
}
