package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OTRO_EMPLEADO;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargoAnulado;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargoDeOtraEmpresa;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.comandoAnular;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoidGeneralChargeOpenAccountService")
class VoidGeneralChargeOpenAccountServiceTest {

    @Mock
    private GeneralChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private VoidGeneralChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<GeneralChargeOpenAccount> cargoCaptor;

    /** Cuenta abierta con el saldo pendiente que se indique. */
    private void cuentaAbiertaConSaldo(String saldo) {
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                .thenReturn(new BigDecimal(saldo));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("marca el cargo como anulado con su autor y motivo")
        void marca_el_cargo_como_anulado() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            cuentaAbiertaConSaldo("50000");
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_EMPLEADO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            verify(repository).save(cargoCaptor.capture());
            GeneralChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.isVoided()).isTrue();
            assertThat(guardado.getVoidedBy()).isEqualTo(OTRO_EMPLEADO);
            assertThat(guardado.getVoidReason()).isEqualTo("Cobrado por error");
            // Anular no borra: la fila sigue habilitada en el historico de la cuenta.
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("bloquea la cuenta, comprueba la version y refresca el total")
        void bloquea_comprueba_version_y_refresca() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            cuentaAbiertaConSaldo("50000");
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_EMPLEADO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID);
            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("el saldo no puede quedar negativo")
    class SaldoNegativo {

        @Test
        @DisplayName("con abonos que cubren el cargo, la anulacion se rechaza")
        void con_abonos_que_cubren_el_cargo_se_rechaza() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            // El cargo vale 11.900 y solo quedan 1.000 pendientes: el resto ya se abono.
            cuentaAbiertaConSaldo("1000");

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("el saldo pendiente quedaría negativo");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, employeeQueryPort);
        }

        @Test
        @DisplayName("con el saldo justo, la anulacion se permite")
        void con_el_saldo_justo_se_permite() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            // Frontera exacta: saldo == total del cargo. Debe pasar, no fallar.
            cuentaAbiertaConSaldo("11900.00");
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_EMPLEADO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            verify(repository).save(any());
        }

        @Test
        @DisplayName("el limite se compara contra el total, no contra el importe unitario")
        void el_limite_se_compara_contra_el_total() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            // 5.950 es el unitario; el total (5.950 x 2) es 11.900. Con 6.000 de saldo
            // la anulacion tiene que fallar: si pasara, el saldo quedaria en -5.900.
            cuentaAbiertaConSaldo("6000");

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalStateException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("cargo inexistente")
        void cargo_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(GeneralChargeOpenAccountNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, openAccountQueryPort, employeeQueryPort);
        }

        @Test
        @DisplayName("cargo de una cuenta de otra empresa")
        void cargo_de_una_cuenta_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargoDeOtraEmpresa()));

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("general charge does not belong to company");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, openAccountQueryPort);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, employeeQueryPort);
        }

        @Test
        @DisplayName("empleado que anula inexistente")
        void empleado_que_anula_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            cuentaAbiertaConSaldo("50000");
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + OTRO_EMPLEADO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un cargo ya anulado no se anula dos veces")
        void un_cargo_ya_anulado_no_se_anula_dos_veces() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargoAnulado()));
            cuentaAbiertaConSaldo("50000");
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_EMPLEADO));

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(GeneralChargeOpenAccountAlreadyVoidedException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
