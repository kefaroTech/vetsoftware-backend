package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateDebtOpenAccountService")
class ReactivateDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;
    @Mock
    private OpenAccountRefresher refresher;

    @InjectMocks
    private ReactivateDebtOpenAccountService service;

    @Test
    @DisplayName("reactiva, recarga el abono y refresca el total de su cuenta")
    void reactiva_recarga_y_refresca() {
        when(repository.reactivate(PAYMENT_ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abono()));

        DebtOpenAccountDto dto = service.execute(PAYMENT_ID, COMPANY_ID);

        assertThat(dto.id()).isEqualTo(PAYMENT_ID);
        // Reactivar vuelve a descontar del saldo: sin refresh, la cuenta muestra una
        // deuda que ya no incluye el abono que acaba de volver.
        verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
    }

    @Test
    @DisplayName("si no reactivo ninguna fila, el abono no era de esta empresa")
    void si_no_reactivo_ninguna_fila_falla() {
        when(repository.reactivate(PAYMENT_ID, OTRA_COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(PAYMENT_ID, OTRA_COMPANY_ID))
                .isInstanceOf(DebtOpenAccountNotFoundException.class)
                .hasMessageContaining(String.valueOf(PAYMENT_ID));

        verifyNoInteractions(refresher);
    }

    @Test
    @DisplayName("si el update dice que reactivo pero la recarga no lo encuentra, falla")
    void si_la_recarga_no_lo_encuentra_falla() {
        when(repository.reactivate(PAYMENT_ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID)).thenReturn(Optional.empty());

        // Carrera con un borrado concurrente: mejor fallar que refrescar la cuenta con
        // un abono que ya no esta.
        assertThatThrownBy(() -> service.execute(PAYMENT_ID, COMPANY_ID))
                .isInstanceOf(DebtOpenAccountNotFoundException.class);

        verifyNoInteractions(refresher);
    }
}
