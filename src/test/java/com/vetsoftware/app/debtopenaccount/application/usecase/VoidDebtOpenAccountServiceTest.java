package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MONTO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRO_EMPLEADO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoAnulado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoDeOtraEmpresa;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoAnular;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoidDebtOpenAccountService")
class VoidDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;
    @Mock
    private CashPort cashPort;

    @InjectMocks
    private VoidDebtOpenAccountService service;

    @Captor
    private ArgumentCaptor<DebtOpenAccount> abonoCaptor;

    /** Abono vivo, cuenta abierta y el empleado que anula resuelto. */
    private void todoEnOrden() {
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abono()));
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                .thenReturn(Optional.of(OTRO_EMPLEADO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("marca el abono como anulado con su autor y motivo")
        void marca_el_abono_como_anulado() {
            todoEnOrden();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            verify(repository).save(abonoCaptor.capture());
            DebtOpenAccount guardado = abonoCaptor.getValue();
            assertThat(guardado.isVoided()).isTrue();
            assertThat(guardado.getVoidedBy()).isEqualTo(OTRO_EMPLEADO);
            assertThat(guardado.getVoidReason()).isEqualTo("Cobrado por error");
            // Anular no borra: la fila sigue habilitada en el historico de la cuenta.
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("bloquea la cuenta, comprueba la version y refresca el total")
        void bloquea_comprueba_version_y_refresca() {
            todoEnOrden();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID);
            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("efecto en caja")
    class Caja {

        @Test
        @DisplayName("compensa el abono en la caja del que anula, no en la del que cobro")
        void compensa_en_la_caja_del_que_anula() {
            todoEnOrden();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            // El dinero sale de la caja de quien hace la anulacion (OTRO_EMPLEADO), que
            // es la que tiene el efectivo delante, no de la del empleado que cobro.
            verify(cashPort).reversePayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID,
                    PaymentMethod.CASH, MONTO, OTRO_EMPLEADO.id());
        }

        @Test
        @DisplayName("exige caja propia abierta antes de anular, y compensa despues de guardar")
        void exige_caja_abierta_antes_y_compensa_despues() {
            todoEnOrden();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoAnular());

            // requireOpenSession antes: sin caja abierta no se puede devolver el dinero.
            // reversePayment despues del save: si la anulacion falla (p. ej. doble
            // anulacion), la caja no se puede haber movido ya.
            InOrder orden = Mockito.inOrder(cashPort, repository, refresher);
            orden.verify(cashPort).requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID,
                    OTRO_EMPLEADO.id());
            orden.verify(repository).save(any());
            orden.verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            orden.verify(cashPort).reversePayment(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("abono inexistente")
        void abono_inexistente() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, openAccountQueryPort, employeeQueryPort, cashPort);
        }

        @Test
        @DisplayName("abono de una cuenta de otra empresa")
        void abono_de_una_cuenta_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abonoDeOtraEmpresa()));

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debt open account does not belong to company");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, openAccountQueryPort, cashPort);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, employeeQueryPort, cashPort);
        }

        @Test
        @DisplayName("empleado que anula inexistente")
        void empleado_que_anula_inexistente() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + OTRO_EMPLEADO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, cashPort);
        }

        @Test
        @DisplayName("un abono ya anulado no se anula dos veces ni se compensa dos veces")
        void un_abono_ya_anulado_no_se_anula_dos_veces() {
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abonoAnulado()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(OTRO_EMPLEADO));

            assertThatThrownBy(() -> service.execute(comandoAnular()))
                    .isInstanceOf(DebtOpenAccountAlreadyVoidedException.class);

            // Lo critico: la caja no puede haber devuelto el dinero una segunda vez.
            verify(repository, never()).save(any());
            verify(cashPort, never()).reversePayment(any(), any(), any(), any(), any(), any());
            verifyNoInteractions(refresher);
        }
    }
}
