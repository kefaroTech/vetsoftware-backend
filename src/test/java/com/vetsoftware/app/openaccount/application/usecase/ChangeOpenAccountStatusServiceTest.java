package com.vetsoftware.app.openaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.out.ClosedAccountEmissionPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.InvalidOpenAccountStatusTransitionException;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.testsupport.OpenAccountMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cierre/anulacion de la cuenta: el caso de uso mas sensible de la feature,
 * toca dinero y estado a la vez. Cubre las 8 ramas de
 * {@link OpenAccount#changeStatus} tal como las ve el servicio: lock, tenant,
 * version, empleado, transicion y emision del documento SOLO al cerrar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeOpenAccountStatusService")
class ChangeOpenAccountStatusServiceTest {

    @Mock
    private OpenAccountRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private ClosedAccountEmissionPort emissionPort;
    @InjectMocks
    private ChangeOpenAccountStatusService service;

    @Nested
    @DisplayName("cierre")
    class Cierre {

        @Test
        @DisplayName("cierra la cuenta con saldo cero y emite el documento electronico")
        void cierra_la_cuenta_con_saldo_cero_y_emite_el_documento() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(employeeQueryPort.findById(OpenAccountMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(OpenAccountMother.CERRADO_POR));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OpenAccountDto dto = service.execute(OpenAccountMother.comandoCerrar());

            assertThat(dto.status()).isEqualTo(OpenAccountStatus.CLOSE);
            ArgumentCaptor<OpenAccount> captor = ArgumentCaptor.forClass(OpenAccount.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(OpenAccountStatus.CLOSE);
            assertThat(captor.getValue().getClosedBy()).isEqualTo(OpenAccountMother.CERRADO_POR);
            verify(emissionPort).emitForClosedAccount(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID, "DOC_EQUIV_POS", false);
        }

        @Test
        @DisplayName("no se puede cerrar como cobrada una cuenta con saldo pendiente")
        void no_se_puede_cerrar_con_saldo_pendiente() {
            OpenAccount cuenta = OpenAccountMother.abiertaConSaldo("500.00", "0.00");
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(employeeQueryPort.findById(OpenAccountMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(OpenAccountMother.CERRADO_POR));

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoCerrar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("saldo pendiente");

            verify(repository, never()).save(any());
            verifyNoInteractions(emissionPort);
        }
    }

    @Nested
    @DisplayName("cancelacion")
    class Cancelacion {

        @Test
        @DisplayName("cancela con motivo y NO emite ningun documento")
        void cancela_con_motivo_y_no_emite_documento() {
            OpenAccount cuenta = OpenAccountMother.abiertaConSaldo("500.00", "0.00");
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(employeeQueryPort.findById(OpenAccountMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(OpenAccountMother.CERRADO_POR));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OpenAccountDto dto = service.execute(OpenAccountMother.comandoCancelar("Incobrable"));

            assertThat(dto.status()).isEqualTo(OpenAccountStatus.CANCEL);
            assertThat(dto.closeReason()).isEqualTo("Incobrable");
            verifyNoInteractions(emissionPort);
        }

        @Test
        @DisplayName("cancelar sin motivo se rechaza y no escribe")
        void cancelar_sin_motivo_se_rechaza() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(employeeQueryPort.findById(OpenAccountMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(OpenAccountMother.CERRADO_POR));

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoCancelar(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to cancel");

            verify(repository, never()).save(any());
            verifyNoInteractions(emissionPort);
        }
    }

    @Nested
    @DisplayName("validaciones que no deben escribir")
    class Validaciones {

        @Test
        @DisplayName("cuenta inexistente lanza OpenAccountNotFoundException y no toca nada mas")
        void cuenta_inexistente_lanza_y_no_toca_nada_mas() {
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoCerrar()))
                    .isInstanceOf(OpenAccountNotFoundException.class).hasMessageContaining("100");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, emissionPort);
        }

        /**
         * El FOR UPDATE va ahora acotado a la empresa, asi que sobre la cuenta ajena no
         * se llega a tomar el lock pesimista: el desenlace es un 404 y no un mensaje
         * que confirme su existencia en otro tenant. Se comprueba ademas que el
         * servicio no toca la variante ancha.
         */
        @Test
        @DisplayName("una cuenta de otra empresa se rechaza y no toca nada mas")
        void cuenta_de_otra_empresa_se_rechaza() {
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoCerrar()))
                    .isInstanceOf(OpenAccountNotFoundException.class);

            verify(repository, never()).save(any());
            verify(repository, never()).findByIdForUpdate(OpenAccountMother.OPEN_ACCOUNT_ID);
            verifyNoInteractions(employeeQueryPort, emissionPort);
        }

        @Test
        @DisplayName("un expectedVersion que no coincide lanza conflicto de version y no toca nada mas")
        void version_no_coincide_lanza_conflicto() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            ChangeOpenAccountStatusCommand command = new ChangeOpenAccountStatusCommand(
                    OpenAccountMother.OPEN_ACCOUNT_ID, "CLOSE", OpenAccountMother.EMPLOYEE_ID, null,
                    OpenAccountMother.COMPANY_ID, null, false, 99L);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(OpenAccountVersionConflictException.class)
                    .hasMessageContaining("100").hasMessageContaining("expected 99")
                    .hasMessageContaining("current is 1");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, emissionPort);
        }

        @Test
        @DisplayName("un empleado inexistente se rechaza y no escribe")
        void empleado_inexistente_se_rechaza() {
            OpenAccount cuenta = OpenAccountMother.abierta();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(employeeQueryPort.findById(OpenAccountMother.EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoCerrar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + OpenAccountMother.EMPLOYEE_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(emissionPort);
        }

        @Test
        @DisplayName("una transicion invalida se rechaza y no escribe ni emite")
        void transicion_invalida_se_rechaza() {
            OpenAccount cuenta = OpenAccountMother.cerrada();
            when(repository.findByIdForUpdateAndCompanyId(OpenAccountMother.OPEN_ACCOUNT_ID,
                    OpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cuenta));
            when(employeeQueryPort.findById(OpenAccountMother.EMPLOYEE_ID))
                    .thenReturn(Optional.of(OpenAccountMother.CERRADO_POR));

            assertThatThrownBy(() -> service.execute(OpenAccountMother.comandoCerrar()))
                    .isInstanceOf(InvalidOpenAccountStatusTransitionException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(emissionPort);
        }
    }
}
