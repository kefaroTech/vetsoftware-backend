package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.vetsoftware.app.openaccount.application.port.in.AssertOpenAccountVersionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAccountVersionGuardAdapter (generalchargeopenaccount)")
class OpenAccountVersionGuardAdapterTest {

    @Mock
    private AssertOpenAccountVersionUseCase assertVersionUseCase;

    @InjectMocks
    private OpenAccountVersionGuardAdapter adapter;

    @Test
    @DisplayName("assertVersion delega en el caso de uso con los mismos argumentos")
    void assert_version_delega_en_el_caso_de_uso() {
        adapter.assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, 3L);

        verify(assertVersionUseCase).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, 3L);
        verifyNoMoreInteractions(assertVersionUseCase);
    }

    @Test
    @DisplayName("un expectedVersion null tambien se delega tal cual: el opt-in decide el use case")
    void un_expected_version_null_se_delega_tal_cual() {
        adapter.assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);

        verify(assertVersionUseCase).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
    }
}
