package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssertOpenAccountVersionService")
class AssertOpenAccountVersionServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @InjectMocks
    private AssertOpenAccountVersionService service;

    @Test
    @DisplayName("un expectedVersion null es opt-in: no consulta el repositorio")
    void expected_version_null_no_consulta_el_repositorio() {
        assertThatCode(() -> service.assertVersion(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.OPEN_ACCOUNT_ID, null)).doesNotThrowAnyException();

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("cuando la version coincide no lanza")
    void version_coincide_no_lanza() {
        OpenAccount cuenta = OpenAccountMother.abierta();
        when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));

        assertThatCode(() -> service.assertVersion(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.OPEN_ACCOUNT_ID, cuenta.getVersion())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cuando la version no coincide lanza OpenAccountVersionConflictException")
    void version_no_coincide_lanza() {
        OpenAccount cuenta = OpenAccountMother.abierta();
        when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> service.assertVersion(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.OPEN_ACCOUNT_ID, 99L))
                .isInstanceOf(OpenAccountVersionConflictException.class).hasMessageContaining("100")
                .hasMessageContaining("expected 99");
    }

    @Test
    @DisplayName("una cuenta ajena o inexistente lanza OpenAccountNotFoundException")
    void cuenta_inexistente_lanza_not_found() {
        when(repository.findByIdAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertVersion(OpenAccountMother.COMPANY_ID,
                OpenAccountMother.OPEN_ACCOUNT_ID, 1L))
                .isInstanceOf(OpenAccountNotFoundException.class);
    }
}
