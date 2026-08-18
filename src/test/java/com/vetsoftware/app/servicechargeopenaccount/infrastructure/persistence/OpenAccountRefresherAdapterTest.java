package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.vetsoftware.app.openaccount.application.port.in.RecalculateOpenAccountUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAccountRefresherAdapter (servicechargeopenaccount)")
class OpenAccountRefresherAdapterTest {

    @Mock
    private RecalculateOpenAccountUseCase recalculateOpenAccountUseCase;

    @InjectMocks
    private OpenAccountRefresherAdapter adapter;

    @Test
    @DisplayName("refresh delega el recalculo con la company y la cuenta recibidas")
    void refresh_delega_el_recalculo_con_la_company_y_la_cuenta() {
        adapter.refresh(COMPANY_ID, OPEN_ACCOUNT_ID);

        verify(recalculateOpenAccountUseCase).recalculate(COMPANY_ID, OPEN_ACCOUNT_ID);
        verifyNoMoreInteractions(recalculateOpenAccountUseCase);
    }
}
