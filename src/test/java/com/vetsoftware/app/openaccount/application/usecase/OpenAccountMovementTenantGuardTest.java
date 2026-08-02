package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.usecase.ListDebtOpenAccountsByOpenAccountService;
import com.vetsoftware.app.generalchargeopenaccount.application.usecase.ListGeneralChargeOpenAccountsByOpenAccountService;
import com.vetsoftware.app.productchargeopenaccount.application.usecase.ListProductChargeOpenAccountsByOpenAccountService;
import com.vetsoftware.app.servicechargeopenaccount.application.usecase.ListServiceChargeOpenAccountsByOpenAccountService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAccountMovementTenantGuardTest {

    private static final long OPEN_ACCOUNT_ID = 10L;
    private static final long COMPANY_ID = 20L;

    @Mock
    private com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository productChargeRepository;

    @Mock
    private com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository serviceChargeRepository;

    @Mock
    private com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository generalChargeRepository;

    @Mock
    private com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository debtRepository;

    @Test
    void productChargesByOpenAccountUseCompanyScope() {
        when(productChargeRepository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of());

        ListProductChargeOpenAccountsByOpenAccountService service = new ListProductChargeOpenAccountsByOpenAccountService(
                productChargeRepository);

        assertThat(service.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)).isEmpty();
        verify(productChargeRepository).findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID,
                COMPANY_ID);
    }

    @Test
    void serviceChargesByOpenAccountUseCompanyScope() {
        when(serviceChargeRepository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of());

        ListServiceChargeOpenAccountsByOpenAccountService service = new ListServiceChargeOpenAccountsByOpenAccountService(
                serviceChargeRepository);

        assertThat(service.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)).isEmpty();
        verify(serviceChargeRepository).findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID,
                COMPANY_ID);
    }

    @Test
    void generalChargesByOpenAccountUseCompanyScope() {
        when(generalChargeRepository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of());

        ListGeneralChargeOpenAccountsByOpenAccountService service = new ListGeneralChargeOpenAccountsByOpenAccountService(
                generalChargeRepository);

        assertThat(service.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)).isEmpty();
        verify(generalChargeRepository).findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID,
                COMPANY_ID);
    }

    @Test
    void debtPaymentsByOpenAccountUseCompanyScope() {
        when(debtRepository.findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of());

        ListDebtOpenAccountsByOpenAccountService service = new ListDebtOpenAccountsByOpenAccountService(
                debtRepository);

        assertThat(service.listByOpenAccount(OPEN_ACCOUNT_ID, COMPANY_ID)).isEmpty();
        verify(debtRepository).findByOpenAccountIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID);
    }
}
