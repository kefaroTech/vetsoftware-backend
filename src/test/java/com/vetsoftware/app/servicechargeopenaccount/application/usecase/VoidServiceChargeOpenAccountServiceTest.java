package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OTRO_EMPLEADO;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargoAnulado;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargoDeOtraEmpresa;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.comandoAnular;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicechargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
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
@DisplayName("VoidServiceChargeOpenAccountService")
class VoidServiceChargeOpenAccountServiceTest {

    @Mock
    private ServiceChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private VoidServiceChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<ServiceChargeOpenAccount> cargoCaptor;

    /** Cuenta abierta con saldo de sobra para absorber la anulacion. */
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
            ServiceChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.isVoided()).isTrue();
            assertThat(guardado.getVoidedBy()).isEqualTo(OTRO_EMPLEADO);
            assertThat(guardado.getVoidReason()).isEqualTo("Cobrado por error");
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

            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
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
            // Frontera exacta: saldo == precio del cargo. Debe pasar, no fallar.
            cuentaAbiertaConSaldo("11900");
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_EMPLEADO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            verify(repository).save(any());
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
                    .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class);

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
                    .hasMessageContaining("service charge does not belong to company");

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
                    .isInstanceOf(ServiceChargeOpenAccountAlreadyVoidedException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
