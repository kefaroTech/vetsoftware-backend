package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateServiceChargeOpenAccountService")
class ReactivateServiceChargeOpenAccountServiceTest {

    @Mock
    private ServiceChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountRefresher refresher;

    @InjectMocks
    private ReactivateServiceChargeOpenAccountService service;

    @Test
    @DisplayName("reactiva, recarga el cargo y refresca el total de su cuenta")
    void reactiva_recarga_y_refresca() {
        when(repository.reactivate(CHARGE_ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                .thenReturn(Optional.of(cargo()));

        ServiceChargeOpenAccountDto dto = service.execute(CHARGE_ID, COMPANY_ID);

        assertThat(dto.id()).isEqualTo(CHARGE_ID);
        // Reactivar vuelve a sumar al total de la cuenta: sin refresh, la cuenta
        // muestra un total que no incluye el cargo que acaba de volver.
        verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
    }

    @Test
    @DisplayName("si no reactivo ninguna fila, el cargo no era de esta empresa")
    void si_no_reactivo_ninguna_fila_falla() {
        when(repository.reactivate(CHARGE_ID, OTRA_COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(CHARGE_ID, OTRA_COMPANY_ID))
                .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class)
                .hasMessageContaining(String.valueOf(CHARGE_ID));

        verifyNoInteractions(refresher);
    }

    @Test
    @DisplayName("si el update dice que reactivo pero la recarga no lo encuentra, falla")
    void si_la_recarga_no_lo_encuentra_falla() {
        when(repository.reactivate(CHARGE_ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID)).thenReturn(Optional.empty());

        // Carrera con un borrado concurrente: mejor fallar que refrescar la cuenta con
        // un cargo que ya no esta.
        assertThatThrownBy(() -> service.execute(CHARGE_ID, COMPANY_ID))
                .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class);

        verifyNoInteractions(refresher);
    }
}
