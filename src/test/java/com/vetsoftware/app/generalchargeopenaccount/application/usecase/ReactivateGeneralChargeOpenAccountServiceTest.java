package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateGeneralChargeOpenAccountService")
class ReactivateGeneralChargeOpenAccountServiceTest {

    @Mock
    private GeneralChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountRefresher refresher;

    @InjectMocks
    private ReactivateGeneralChargeOpenAccountService service;

    @Test
    @DisplayName("reactiva, recarga el cargo y refresca el total de su cuenta")
    void reactiva_recarga_y_refresca() {
        when(repository.reactivate(CHARGE_ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                .thenReturn(Optional.of(cargo()));

        GeneralChargeOpenAccountDto dto = service.execute(CHARGE_ID, COMPANY_ID);

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
                .isInstanceOf(GeneralChargeOpenAccountNotFoundException.class)
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
                .isInstanceOf(GeneralChargeOpenAccountNotFoundException.class);

        verifyNoInteractions(refresher);
    }
}
