package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.openaccount.application.port.in.RecalculateOpenAccountUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAccountRefresherAdapter (debtopenaccount) — delega el recalculo en el caso de uso de openaccount")
class OpenAccountRefresherAdapterTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OPEN_ACCOUNT_ID = 50L;

    @Mock
    private RecalculateOpenAccountUseCase recalculateOpenAccountUseCase;

    @InjectMocks
    private OpenAccountRefresherAdapter adapter;

    @Test
    @DisplayName("refresh delega en recalculate con la empresa y la cuenta recibidas")
    void refresh_delega_en_recalculate() {
        adapter.refresh(COMPANY_ID, OPEN_ACCOUNT_ID);

        verify(recalculateOpenAccountUseCase).recalculate(COMPANY_ID, OPEN_ACCOUNT_ID);
    }
}
