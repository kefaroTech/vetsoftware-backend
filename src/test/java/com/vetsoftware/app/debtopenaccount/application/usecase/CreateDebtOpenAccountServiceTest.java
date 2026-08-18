package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.CUENTA;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.EMPLEADO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MONTO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.SALDO_PENDIENTE;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoConClave;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoCrear;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoCrearPor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
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
@DisplayName("CreateDebtOpenAccountService")
class CreateDebtOpenAccountServiceTest {

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
    private CreateDebtOpenAccountService service;

    @Captor
    private ArgumentCaptor<DebtOpenAccount> abonoCaptor;

    /**
     * Cuenta abierta, de la empresa correcta, con 50.000 pendientes y el empleado
     * resuelto.
     */
    private void todoEnOrden() {
        when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(CUENTA));
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                .thenReturn(new BigDecimal(SALDO_PENDIENTE));
        when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID))
                .thenReturn(Optional.of(EMPLEADO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda el abono con la cuenta y el empleado resueltos por los puertos")
        void guarda_el_abono_con_las_referencias_resueltas() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear());

            verify(repository).save(abonoCaptor.capture());
            DebtOpenAccount guardado = abonoCaptor.getValue();
            // Las refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
            assertThat(guardado.getCreatedBy()).isEqualTo(EMPLEADO);
            assertThat(guardado.getAmount()).isEqualByComparingTo("30000");
            assertThat(guardado.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(guardado.getId()).as("el abono nuevo no trae id").isNull();
        }

        @Test
        @DisplayName("devuelve el DTO del abono persistido, con su id")
        void devuelve_el_dto_del_abono_persistido() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono(777L));

            DebtOpenAccountDto dto = service.execute(comandoCrear());

            assertThat(dto.id()).isEqualTo(777L);
        }

        @Test
        @DisplayName("comprueba la version esperada de la cuenta")
        void comprueba_la_version_esperada_de_la_cuenta() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear());

            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
        }
    }

    @Nested
    @DisplayName("efecto en caja")
    class Caja {

        @Test
        @DisplayName("exige caja propia abierta ANTES de guardar el abono")
        void exige_caja_propia_abierta_antes_de_guardar() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear());

            // Si el chequeo fuera despues del save, un cobro sin caja abierta ya habria
            // bajado el saldo de la cuenta cuando revienta: dinero cobrado que no esta
            // en ninguna caja.
            InOrder orden = Mockito.inOrder(cashPort, repository);
            orden.verify(cashPort).requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID, EMPLEADO.id());
            orden.verify(repository).save(any());
        }

        @Test
        @DisplayName("registra el abono en caja con el id ya generado por el repositorio")
        void registra_el_abono_en_caja_con_el_id_generado() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear());

            // El id sale del abono persistido, no del comando: es la referencia con la
            // que la caja deduplica el movimiento y con la que se compensa al anular.
            verify(cashPort).registerPayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID,
                    PaymentMethod.CASH, MONTO, EMPLEADO.id());
        }

        @Test
        @DisplayName("bloquea la cuenta primero y refresca su total antes de tocar la caja")
        void bloquea_primero_y_refresca_antes_de_tocar_la_caja() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear());

            // El lock tiene que ir ANTES de leer el saldo: si se toma despues, dos abonos
            // concurrentes leen el mismo pendiente y entre los dos lo dejan negativo.
            InOrder orden = Mockito.inOrder(openAccountQueryPort, repository, refresher, cashPort);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).outstandingAmount(OPEN_ACCOUNT_ID);
            orden.verify(repository).save(any());
            orden.verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            orden.verify(cashPort).registerPayment(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("el abono no puede exceder el saldo pendiente")
    class Sobrepago {

        @Test
        @DisplayName("un abono mayor que el pendiente se rechaza")
        void un_abono_mayor_que_el_pendiente_se_rechaza() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));

            // No se maneja vuelto en cuenta: el cambio en efectivo se da en el POS.
            assertThatThrownBy(() -> service.execute(comandoCrearPor("50001")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede exceder el saldo pendiente");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, cashPort, employeeQueryPort);
        }

        @Test
        @DisplayName("un abono por el pendiente exacto se permite")
        void un_abono_por_el_pendiente_exacto_se_permite() {
            // Frontera exacta: saldar la cuenta de un solo pago es el caso normal, no un
            // sobrepago.
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrearPor(SALDO_PENDIENTE));

            verify(repository).save(abonoCaptor.capture());
            assertThat(abonoCaptor.getValue().getAmount()).isEqualByComparingTo("50000");
        }
    }

    @Nested
    @DisplayName("idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("con una clave ya usada devuelve el abono anterior sin volver a cobrar")
        void con_una_clave_ya_usada_devuelve_el_abono_anterior() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(abonoConClave("req-1")));

            DebtOpenAccountDto dto = service.execute(comandoCrear("req-1"));

            // El reintento legitimo del mismo cliente sigue devolviendo EL MISMO abono:
            // acotar la via de idempotencia no puede costar la idempotencia.
            assertThat(dto.id()).isEqualTo(PAYMENT_ID);
            // Un reintento no puede cobrar dos veces ni meter el mismo dinero en caja.
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, employeeQueryPort, cashPort);
        }

        @Test
        @DisplayName("un reintento con la clave de otra empresa no devuelve su abono")
        void un_reintento_con_la_clave_de_otra_empresa_no_devuelve_su_abono() {
            // La cuenta del comando es de otro tenant, asi que la resolucion acotada no
            // la encuentra. Con el chequeo de idempotencia por delante —como estaba— el
            // finder no lleva empresa (el abono no tiene company_id propio) y bastaba
            // acertar el clientRequestId para que el servicio devolviera el DTO del abono
            // ajeno sin pasar por ninguna comprobacion.
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear("req-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).findByOpenAccountIdAndClientRequestId(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, employeeQueryPort, cashPort);
        }

        @Test
        @DisplayName("la deduplicacion va DESPUES de la resolucion acotada y ANTES del guard")
        void la_deduplicacion_va_despues_de_la_resolucion_acotada() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(abonoConClave("req-1")));

            service.execute(comandoCrear("req-1"));

            // Despues del lock: el reintento que llega segundo tiene que leer el abono ya
            // committeado por el rival. Despues de la resolucion acotada: es lo unico que
            // demuestra que la cuenta es de esta empresa antes de leer sus abonos. Antes
            // del guard de sobrepago y del versionGuard: tras el primer abono el saldo ya
            // bajo y la version subio, y el reintento fallaria en vez de devolver el
            // original.
            InOrder orden = Mockito.inOrder(openAccountQueryPort, repository);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(repository).findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID,
                    "req-1");
            verify(openAccountQueryPort, never()).outstandingAmount(any());
            verifyNoInteractions(versionGuard);
        }

        @Test
        @DisplayName("con una clave nueva sigue el camino normal")
        void con_una_clave_nueva_sigue_el_camino_normal() {
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-nueva"))
                    .thenReturn(Optional.empty());
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear("req-nueva"));

            verify(repository).save(abonoCaptor.capture());
            assertThat(abonoCaptor.getValue().getClientRequestId()).isEqualTo("req-nueva");
        }

        @Test
        @DisplayName("una clave en blanco no activa la deduplicacion")
        void una_clave_en_blanco_no_activa_la_deduplicacion() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(abono());

            service.execute(comandoCrear("   "));

            verify(repository, never()).findByOpenAccountIdAndClientRequestId(any(), any());
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("cuenta inexistente")
        void cuenta_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, cashPort);
        }

        @Test
        @DisplayName("cuenta de otra empresa: no se cobra contra un tenant ajeno")
        void cuenta_de_otra_empresa() {
            // La cuenta existe, pero es de otra empresa: la consulta acotada no la
            // resuelve, asi que el abono se rechaza igual que si no existiera. Antes la
            // cuenta ajena SI se cargaba y solo un if posterior evitaba el cobro.
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            // Si el servicio volviera a la variante ancha, la cuenta ajena entraria de
            // nuevo en el flujo: el abono quedaria escrito en la cartera del otro tenant.
            verify(openAccountQueryPort, never()).findById(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, cashPort);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, cashPort, employeeQueryPort);
        }

        @Test
        @DisplayName("empleado inexistente")
        void empleado_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + EMPLEADO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, cashPort);
        }

        @Test
        @DisplayName("un medio de pago que no existe en el catalogo")
        void un_medio_de_pago_que_no_existe() {
            todoEnOrden();

            // El comando trae el medio como String; el enum es la ultima frontera.
            assertThatThrownBy(() -> service.execute(new CreateDebtOpenAccountCommand(MONTO,
                    "PAYPAL", OPEN_ACCOUNT_ID, COMPANY_ID, EMPLEADO.id(), null, null)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PAYPAL");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
