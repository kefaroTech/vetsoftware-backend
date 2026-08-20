package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.CUENTA;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MONTO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_CUENTA;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_CUENTA_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.SALDO_ESTRECHO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.SALDO_PENDIENTE;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoAnulado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoDeshabilitado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoEnOtraCuenta;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoActualizar;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoActualizarPor;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoTrasladar;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoTrasladarALaCuentaPrincipal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
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
@DisplayName("UpdateDebtOpenAccountService")
class UpdateDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private UpdateDebtOpenAccountService service;

    @Captor
    private ArgumentCaptor<DebtOpenAccount> abonoCaptor;

    /**
     * Edicion normal (sin traslado) de un abono vivo sobre una cuenta abierta con
     * saldo de sobra. Deja fuera el {@code save} a proposito: los caminos de error
     * no llegan a el y con STRICT_STUBS un stub muerto rompe el test.
     */
    private void abonoVivoEnCuentaAbierta() {
        when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(abono()));
        when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(CUENTA));
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                .thenReturn(new BigDecimal(SALDO_PENDIENTE));
    }

    private void devuelveLoGuardado() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("aplica monto y medio de pago nuevos sobre el abono cargado")
        void aplica_los_valores_nuevos() {
            abonoVivoEnCuentaAbierta();
            devuelveLoGuardado();

            service.execute(comandoActualizar());

            verify(repository).save(abonoCaptor.capture());
            DebtOpenAccount guardado = abonoCaptor.getValue();
            assertThat(guardado.getAmount()).isEqualByComparingTo("45000");
            assertThat(guardado.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
        }

        @Test
        @DisplayName("comprueba la version esperada y refresca solo la cuenta destino")
        void comprueba_la_version_y_refresca_solo_la_cuenta_destino() {
            abonoVivoEnCuentaAbierta();
            devuelveLoGuardado();

            service.execute(comandoActualizar());

            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            verify(refresher, never()).refresh(COMPANY_ID, OTRA_CUENTA_ID);
        }

        @Test
        @DisplayName("editar un abono SI bloquea la cuenta y comprueba el saldo pendiente")
        void editar_un_abono_bloquea_la_cuenta_y_comprueba_el_saldo_pendiente() {
            abonoVivoEnCuentaAbierta();
            devuelveLoGuardado();

            service.execute(comandoActualizar());

            // Aqui hubo durante meses un hueco declarado: la edicion no tomaba lock ni
            // validaba sobrepago, asi que un PUT con un importe mayor que el facturado
            // dejaba el saldo en negativo con HTTP 200 y sin concurrencia ninguna. Los
            // issues #110/#123 lo cerraron y este test quedo invertido: lo que antes se
            // afirmaba con never() ahora se exige que ocurra, para que quitar el lock o
            // el guard vuelva a poner el test en rojo.
            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            verify(openAccountQueryPort).outstandingAmount(OPEN_ACCOUNT_ID);
        }

    }

    @Nested
    @DisplayName("orden de bloqueo")
    class Bloqueo {

        /**
         * Las dos cuentas de un traslado se bloquean por id ASCENDENTE, no en el orden
         * en que las trae el comando. Es la unica prueba escrita de que dos traslados
         * cruzados (50 a 51 y 51 a 50 a la vez) no se pueden quedar esperandose: con un
         * orden total unico sobre el recurso no hay ciclo de espera posible. Sin estos
         * dos casos, reordenar los locks pasa la revision y el deadlock aparece en
         * produccion en hora punta.
         */
        @Test
        @DisplayName("traslado 50 -> 51: bloquea 50 y despues 51")
        void traslado_ascendente_bloquea_en_orden_ascendente() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OTRA_CUENTA_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            when(openAccountQueryPort.isOpen(OTRA_CUENTA_ID)).thenReturn(true);
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OTRA_CUENTA_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));
            devuelveLoGuardado();

            service.execute(comandoTrasladar());

            InOrder orden = Mockito.inOrder(repository, openAccountQueryPort);
            orden.verify(repository).lockAndFindOpenAccountId(PAYMENT_ID);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).lockForUpdate(OTRA_CUENTA_ID, COMPANY_ID);
            orden.verify(repository).findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("traslado 51 -> 50: bloquea igualmente 50 antes que 51, no el origen primero")
        void traslado_descendente_bloquea_igualmente_en_orden_ascendente() {
            // El abono vive HOY en la 51 y se lleva a la 50. Si el codigo bloqueara
            // "origen y luego destino" tomaria 51 antes que 50 y este test seria el
            // unico que lo veria: el del sentido contrario pasaria igual.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abonoEnOtraCuenta()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.isOpen(OTRA_CUENTA_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));
            devuelveLoGuardado();

            service.execute(comandoTrasladarALaCuentaPrincipal());

            InOrder orden = Mockito.inOrder(openAccountQueryPort);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).lockForUpdate(OTRA_CUENTA_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("sin traslado hay un unico lock, el de la cuenta del abono")
        void sin_traslado_hay_un_unico_lock() {
            abonoVivoEnCuentaAbierta();
            devuelveLoGuardado();

            service.execute(comandoActualizar());

            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            verify(openAccountQueryPort, never()).lockForUpdate(OTRA_CUENTA_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("el lock del abono es la PRIMERA sentencia, antes de cualquier lectura")
        void el_lock_del_abono_es_la_primera_sentencia() {
            abonoVivoEnCuentaAbierta();
            devuelveLoGuardado();

            service.execute(comandoActualizar());

            // La lectura plana del abono abre el snapshot REPEATABLE READ. Si llegara
            // antes que el lock, el guard de sobrepago y el recalculo leerian el saldo
            // de ANTES de esperar al lock: perdida de actualizacion silenciosa.
            InOrder orden = Mockito.inOrder(repository, openAccountQueryPort);
            orden.verify(repository).lockAndFindOpenAccountId(PAYMENT_ID);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(repository).findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).outstandingAmount(OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("guard de sobrepago: el margen incluye el importe actual del propio abono")
    class Sobrepago {

        private void abonoConSaldoEstrecho(DebtOpenAccount existente) {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal(SALDO_ESTRECHO));
        }

        @Test
        @DisplayName("subir el importe dentro del margen del propio abono se acepta")
        void subir_el_importe_dentro_del_margen_se_acepta() {
            // Saldo 10.000 y el abono vale hoy 30.000: su importe viejo YA esta restado
            // dentro de outstanding, asi que el margen real es 40.000 y subirlo a 35.000
            // es legitimo. La formula ingenua (comparar contra 10.000 a secas) lo
            // rechazaria y dejaria al cajero sin poder corregir un cobro mal tecleado.
            abonoConSaldoEstrecho(abono());
            devuelveLoGuardado();

            service.execute(comandoActualizarPor("35000"));

            verify(repository).save(abonoCaptor.capture());
            assertThat(abonoCaptor.getValue().getAmount()).isEqualByComparingTo("35000");
        }

        @Test
        @DisplayName("el importe exactamente igual al margen se acepta")
        void el_importe_igual_al_margen_se_acepta() {
            abonoConSaldoEstrecho(abono());
            devuelveLoGuardado();

            service.execute(comandoActualizarPor("40000"));

            verify(repository).save(abonoCaptor.capture());
            assertThat(abonoCaptor.getValue().getAmount()).isEqualByComparingTo("40000");
        }

        @Test
        @DisplayName("pasarse del margen es sobrepago y se rechaza sin escribir nada")
        void pasarse_del_margen_se_rechaza() {
            abonoConSaldoEstrecho(abono());

            assertThatThrownBy(() -> service.execute(comandoActualizarPor("40001")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede exceder el saldo pendiente");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un abono anulado no aporta margen: su importe no cuenta en el saldo")
        void un_abono_anulado_no_aporta_margen() {
            // El abono esta anulado, asi que NO esta restado dentro de outstanding:
            // sumarlo al margen regalaria 30.000 de sobrepago.
            abonoConSaldoEstrecho(abonoAnulado());

            assertThatThrownBy(() -> service.execute(comandoActualizarPor("35000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede exceder el saldo pendiente");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un abono deshabilitado tampoco aporta margen")
        void un_abono_deshabilitado_no_aporta_margen() {
            abonoConSaldoEstrecho(abonoDeshabilitado());

            assertThatThrownBy(() -> service.execute(comandoActualizarPor("35000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede exceder el saldo pendiente");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("en un traslado el abono no aporta margen en la cuenta destino")
        void en_un_traslado_el_abono_no_aporta_margen_en_el_destino() {
            // El abono todavia no suma en la cuenta destino, asi que el margen alli es
            // el saldo pendiente a secas: mover 30.000 a una cuenta que solo debe 10.000
            // la dejaria en negativo.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OTRA_CUENTA_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            when(openAccountQueryPort.isOpen(OTRA_CUENTA_ID)).thenReturn(true);
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OTRA_CUENTA_ID))
                    .thenReturn(new BigDecimal(SALDO_ESTRECHO));

            assertThatThrownBy(() -> service.execute(comandoTrasladar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede exceder el saldo pendiente");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }

    @Nested
    @DisplayName("traslado entre cuentas")
    class Traslado {

        @Test
        @DisplayName("refresca las DOS cuentas: la vieja tambien cambia de saldo")
        void refresca_las_dos_cuentas() {
            // El abono vive hoy en CUENTA (50) y el comando lo mueve a OTRA_CUENTA (51).
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OTRA_CUENTA_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            when(openAccountQueryPort.isOpen(OTRA_CUENTA_ID)).thenReturn(true);
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OTRA_CUENTA_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));
            devuelveLoGuardado();

            service.execute(comandoTrasladar());

            // Sin el refresh de la cuenta de origen, esa cuenta sigue descontando un
            // abono que se llevaron a otro sitio: el cliente ve menos deuda de la que
            // tiene.
            verify(refresher).refresh(COMPANY_ID, OTRA_CUENTA_ID);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("la cuenta de ORIGEN cerrada tambien bloquea el traslado")
        void la_cuenta_de_origen_cerrada_bloquea_el_traslado() {
            // En un traslado se reescribe el saldo de las dos cuentas, asi que la de
            // origen tiene que estar tan abierta como la destino: si no, el traslado le
            // reescribe el saldo a una cuenta ya cerrada o cancelada.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OTRA_CUENTA_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            when(openAccountQueryPort.isOpen(OTRA_CUENTA_ID)).thenReturn(true);
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoTrasladar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("source open account is not OPEN");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("abono inexistente o de otra empresa")
        void abono_inexistente_o_de_otra_empresa() {
            // El lock del abono es la primera sentencia y no devuelve fila: la
            // transaccion muere antes de tocar ninguna cuenta, y por eso el
            // verifyNoInteractions sobre el puerto de cuentas sigue siendo cierto.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PAYMENT_ID));

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, openAccountQueryPort);
        }

        @Test
        @DisplayName("abono que existe pero es de otra empresa: la carga acotada no lo resuelve")
        void abono_de_otra_empresa_no_pasa_la_carga_acotada() {
            // El lock es ancho a proposito (bloquea una sola fila de abonos, ninguna
            // cuenta); lo que convierte ese lock en un 404 con rollback es la carga
            // acotada por empresa de la sentencia siguiente.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PAYMENT_ID));

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta destino que ya no esta abierta")
        void cuenta_destino_que_ya_no_esta_abierta() {
            // Una CLOSE tiene saldo cero por invariante y una CANCEL es una perdida ya
            // contabilizada: reescribirles el saldo desde una edicion las corrompe en
            // silencio.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(openAccountQueryPort, never()).outstandingAmount(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("cuenta destino inexistente")
        void cuenta_destino_inexistente() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta destino de otra empresa: el abono no se traslada a otro tenant")
        void cuenta_destino_de_otra_empresa() {
            // La cuenta destino existe pero es de otra empresa: la consulta acotada no la
            // resuelve, asi que el traslado se rechaza. Antes la cuenta ajena SI se
            // cargaba y solo un if posterior impedia mover el abono a su cartera.
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(openAccountQueryPort, never()).findById(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
        }

        @Test
        @DisplayName("un monto invalido no deja el abono a medias ni lo guarda")
        void un_monto_invalido_no_deja_el_abono_a_medias() {
            DebtOpenAccount existente = abono();
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));

            assertThatThrownBy(() -> service.execute(new UpdateDebtOpenAccountCommand(PAYMENT_ID,
                    BigDecimal.ZERO, "CASH", OPEN_ACCOUNT_ID, COMPANY_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("amount must be positive");

            assertThat(existente.getAmount()).isEqualByComparingTo(MONTO);
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un medio de pago que no existe en el catalogo")
        void un_medio_de_pago_que_no_existe() {
            when(repository.lockAndFindOpenAccountId(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(abono()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                    .thenReturn(new BigDecimal(SALDO_PENDIENTE));

            assertThatThrownBy(() -> service.execute(new UpdateDebtOpenAccountCommand(PAYMENT_ID,
                    MONTO, "CHEQUE", OPEN_ACCOUNT_ID, COMPANY_ID, null)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CHEQUE");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
