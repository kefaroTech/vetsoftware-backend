package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MONTO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MOTIVO_BAJA;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRO_EMPLEADO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoAnulado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoDeOtraEmpresa;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoBorrar;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoBorrarDesdeOtraEmpresa;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoBorrarPor;
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

/**
 * Quitar un abono es mover dinero: sube el saldo pendiente de la cuenta y deja
 * en la caja un ingreso que ya no corresponde a ningun cobro. Desde los issues
 * #110/#123 la baja es <b>una anulacion mas el ocultado de la fila</b> y exige
 * lo mismo que anular: lock del abono como primera sentencia, lock de la
 * cuenta, version esperada, cuenta abierta, motivo obligatorio y compensacion
 * de caja.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteDebtOpenAccountService")
class DeleteDebtOpenAccountServiceTest {

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
    private DeleteDebtOpenAccountService service;

    @Captor
    private ArgumentCaptor<DebtOpenAccount> abonoCaptor;

    /** Abono vivo, cuenta abierta y el empleado que da la baja resuelto. */
    private void todoEnOrden(DebtOpenAccount existente) {
        when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(existente));
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                .thenReturn(Optional.of(OTRO_EMPLEADO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("la baja deja el abono anulado con su autor y su motivo antes de ocultarlo")
        void la_baja_deja_el_abono_anulado_con_autor_y_motivo() {
            todoEnOrden(abono());

            service.execute(comandoBorrar());

            // Sin esto el "reason" seria un parametro muerto y la baja de un cobro
            // quedaria sin rastro de quien la hizo ni por que.
            verify(repository).save(abonoCaptor.capture());
            DebtOpenAccount guardado = abonoCaptor.getValue();
            assertThat(guardado.isVoided()).isTrue();
            assertThat(guardado.getVoidedBy()).isEqualTo(OTRO_EMPLEADO);
            assertThat(guardado.getVoidReason()).isEqualTo(MOTIVO_BAJA);
        }

        @Test
        @DisplayName("bloquea la cuenta, comprueba la version y refresca el total")
        void bloquea_comprueba_version_y_refresca() {
            todoEnOrden(abono());

            service.execute(comandoBorrar());

            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
            verify(repository).delete(PAYMENT_ID, COMPANY_ID);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("el lock del abono es la PRIMERA sentencia, antes de leer nada")
        void el_lock_del_abono_es_la_primera_sentencia() {
            todoEnOrden(abono());

            service.execute(comandoBorrar());

            // La carga plana del abono trae la cuenta por @EntityGraph y fija el
            // snapshot REPEATABLE READ: si llegara antes que los locks, el recalculo
            // sumaria los abonos de ANTES de esperar al lock y pisaria el saldo ajeno
            // sin excepcion y sin log.
            InOrder orden = Mockito.inOrder(repository, openAccountQueryPort);
            orden.verify(repository).lockAndFindOpenAccountId(PAYMENT_ID);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(repository).findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("efecto en caja")
    class Caja {

        @Test
        @DisplayName("compensa el abono en la caja del que da la baja, no en la del que cobro")
        void compensa_en_la_caja_del_que_da_la_baja() {
            todoEnOrden(abono());

            service.execute(comandoBorrar());

            verify(cashPort).reversePayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID,
                    PaymentMethod.CASH, MONTO, OTRO_EMPLEADO.id());
        }

        @Test
        @DisplayName("exige caja propia abierta antes, y compensa despues del borrado")
        void exige_caja_abierta_antes_y_compensa_despues() {
            todoEnOrden(abono());

            service.execute(comandoBorrar());

            InOrder orden = Mockito.inOrder(cashPort, repository, refresher);
            orden.verify(cashPort).requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID,
                    OTRO_EMPLEADO.id());
            orden.verify(repository).save(any());
            orden.verify(repository).delete(PAYMENT_ID, COMPANY_ID);
            orden.verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            orden.verify(cashPort).reversePayment(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("un abono YA anulado se oculta sin volver a compensar la caja")
        void un_abono_ya_anulado_no_se_compensa_dos_veces() {
            // Su compensacion se hizo al anularlo. Volver a compensar aqui descuadraria
            // la caja del actor por el importe del abono.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abonoAnulado()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);

            service.execute(comandoBorrar());

            verify(repository).delete(PAYMENT_ID, COMPANY_ID);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, cashPort);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("abono inexistente: el lock no devuelve fila y no se toca ninguna cuenta")
        void abono_inexistente() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoBorrar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PAYMENT_ID));

            verify(repository, never()).delete(any(), any());
            verifyNoInteractions(openAccountQueryPort, employeeQueryPort, refresher, versionGuard,
                    cashPort);
        }

        @Test
        @DisplayName("abono de otra empresa: la carga acotada lo convierte en 404")
        void abono_de_otra_empresa() {
            // El lock del abono es ancho a proposito (bloquea una fila de abonos, no
            // cuentas); lo que impide borrar el abono del vecino es la carga acotada.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoBorrarDesdeOtraEmpresa()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PAYMENT_ID));

            verify(repository, never()).delete(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, refresher, versionGuard, cashPort);
        }

        @Test
        @DisplayName("abono cuya cuenta es de otra empresa: guard de tenancy, no se borra")
        void abono_cuya_cuenta_es_de_otra_empresa() {
            // Defensa en profundidad: la lectura ya viene acotada, pero si algun dia deja
            // de estarlo, este guard impide borrar el abono de otro tenant.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abonoDeOtraEmpresa()));

            assertThatThrownBy(() -> service.execute(comandoBorrar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("debt open account does not belong to company");

            verify(repository, never()).delete(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, refresher, versionGuard, cashPort);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoBorrar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(repository, never()).delete(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, refresher, cashPort);
        }

        @Test
        @DisplayName("version esperada distinta: conflicto antes de mover nada")
        void version_esperada_distinta() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            Mockito.doThrow(new IllegalStateException("open account version mismatch"))
                    .when(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);

            assertThatThrownBy(() -> service.execute(comandoBorrar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("version mismatch");

            verify(repository, never()).delete(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, refresher, cashPort);
        }

        @Test
        @DisplayName("empleado que da la baja inexistente o de otra empresa")
        void empleado_que_da_la_baja_inexistente() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoBorrar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + OTRO_EMPLEADO.id());

            verify(repository, never()).delete(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, cashPort);
        }

        @Test
        @DisplayName("sin motivo la baja no se registra: ni se anula, ni se borra, ni se compensa")
        void sin_motivo_la_baja_no_se_registra() {
            // El motivo se persiste como motivo de anulacion; sin el, la baja de un cobro
            // quedaria sin rastro auditable.
            todoEnOrden(abono());

            assertThatThrownBy(() -> service.execute(comandoBorrarPor("  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to void");

            verify(repository, never()).delete(any(), any());
            verify(repository, never()).save(any());
            verify(cashPort, never()).reversePayment(any(), any(), any(), any(), any(), any());
            verifyNoInteractions(refresher);
        }
    }
}
