package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindOpenAccountService")
class FindOpenAccountServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private FindOpenAccountService service;

    @Test
    @DisplayName("encuentra la cuenta de la empresa y la mapea a dto")
    void encuentra_la_cuenta_y_la_mapea() {
        when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(OpenAccountMother.abierta()));

        OpenAccountDto dto = service.findById(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(OpenAccountMother.OPEN_ACCOUNT_ID);
    }

    @Test
    @DisplayName("una cuenta inexistente o de otra empresa lanza OpenAccountNotFoundException")
    void cuenta_inexistente_lanza_not_found() {
        when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).isInstanceOf(OpenAccountNotFoundException.class);
    }
}
