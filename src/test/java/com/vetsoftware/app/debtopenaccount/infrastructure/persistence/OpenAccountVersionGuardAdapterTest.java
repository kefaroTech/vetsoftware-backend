package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.openaccount.application.port.in.AssertOpenAccountVersionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAccountVersionGuardAdapter (debtopenaccount) — delega el guard de version en el caso de uso de openaccount")
class OpenAccountVersionGuardAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OPEN_ACCOUNT_ID = 50L;
    private static final Long EXPECTED_VERSION = 3L;

    @Mock
    private AssertOpenAccountVersionUseCase assertVersionUseCase;

    @InjectMocks
    private OpenAccountVersionGuardAdapter adapter;

    @Test
    @DisplayName("assertVersion delega en assertVersion con la empresa, la cuenta y la version esperada")
    void assert_version_delega_en_el_caso_de_uso() {
        adapter.assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, EXPECTED_VERSION);

        verify(assertVersionUseCase).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, EXPECTED_VERSION);
    }
}
