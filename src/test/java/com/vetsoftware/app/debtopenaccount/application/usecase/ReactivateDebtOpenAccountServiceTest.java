package com.vetsoftware.app.debtopenaccount.application.usecase;

import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.MONTO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.OTRO_EMPLEADO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.PAYMENT_ID;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.SALDO_ESTRECHO;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.SALDO_PENDIENTE;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abono;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoAnulado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoDeshabilitado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.abonoDeshabilitadoYAnulado;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoReactivar;
import static com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother.comandoReactivarDesdeOtraEmpresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reactivar un abono es una operacion de DINERO: vuelve a descontarlo del saldo
 * pendiente de la cuenta y vuelve a contarlo como cobrado. Este caso de uso
 * eran nueve lineas sin una sola guarda —ni lock, ni {@code isOpen}, ni
 * sobrepago, ni caja— y ningun test lo veia (#218).
 *
 * <p>
 * Lo que se fija aqui es lo que faltaba, y en particular <b>el orden</b>:
 * bloquear, comprobar y solo entonces mutar. Verificar que las llamadas
 * <em>existen</em> no distingue el arreglo del defecto —la version vieja
 * tambien recargaba y refrescaba, solo que despues de haber encendido la fila—,
 * asi que las secuencias van con {@code InOrder}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateDebtOpenAccountService")
class ReactivateDebtOpenAccountServiceTest {

    @Mock
    private DebtOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private CashPort cashPort;

    @InjectMocks
    private ReactivateDebtOpenAccountService service;

    /** El abono apagado existe y cuelga de una cuenta de esta empresa. */
    private void elAbonoApagadoCuelgaDeLaCuenta(DebtOpenAccount apagado) {
        when(repository.lockAndFindOpenAccountIdIncludingDisabled(PAYMENT_ID))
                .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
        when(repository.findByIdIncludingDisabledAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(apagado));
    }

    private void laCuentaSigueAbierta() {
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
    }

    private void saldoPendiente(String importe) {
        when(openAccountQueryPort.outstandingAmount(OPEN_ACCOUNT_ID))
                .thenReturn(new BigDecimal(importe));
    }

    private void elUpdateEnciendeLaFila(DebtOpenAccount recargado) {
        when(repository.reactivate(PAYMENT_ID, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(recargado));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva, recarga el abono y refresca el total de su cuenta")
        void reactiva_recarga_y_refresca() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_PENDIENTE);
            elUpdateEnciendeLaFila(abono());

            DebtOpenAccountDto dto = service.execute(comandoReactivar());

            assertThat(dto.id()).isEqualTo(PAYMENT_ID);
            // Reactivar vuelve a descontar del saldo: sin refresh, la cuenta muestra una
            // deuda que ya no incluye el abono que acaba de volver.
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("el orden es bloquear, comprobar y SOLO ENTONCES mutar")
        void el_orden_es_bloquear_comprobar_y_solo_entonces_mutar() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_PENDIENTE);
            elUpdateEnciendeLaFila(abono());

            service.execute(comandoReactivar());

            // La version vieja hacia el UPDATE PRIMERO y preguntaba despues, asi que
            // verificar que las llamadas existen no la habria distinguido del arreglo.
            // Lo que hay que fijar es la secuencia: la lectura de bloqueo abre el caso de
            // uso —no una lectura plana, que fijaria el snapshot REPEATABLE READ antes
            // del lock y dejaria al saldo leyendo lo de antes de esperarlo—, luego el
            // lock de la cuenta, luego las guardas, y la fila se enciende la ultima.
            InOrder enOrden = inOrder(repository, openAccountQueryPort, refresher, cashPort);
            enOrden.verify(repository).lockAndFindOpenAccountIdIncludingDisabled(PAYMENT_ID);
            enOrden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            enOrden.verify(repository).findByIdIncludingDisabledAndCompanyId(PAYMENT_ID,
                    COMPANY_ID);
            enOrden.verify(openAccountQueryPort).isOpen(OPEN_ACCOUNT_ID);
            enOrden.verify(openAccountQueryPort).outstandingAmount(OPEN_ACCOUNT_ID);
            enOrden.verify(cashPort).requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID,
                    OTRO_EMPLEADO.id());
            enOrden.verify(repository).reactivate(PAYMENT_ID, COMPANY_ID);
            enOrden.verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            enOrden.verify(cashPort).registerPayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID,
                    PaymentMethod.CASH, MONTO, OTRO_EMPLEADO.id());
        }
    }

    @Nested
    @DisplayName("una cuenta que ya no esta abierta no admite abonos de vuelta")
    class CuentaNoAbierta {

        @Test
        @DisplayName("sobre una cuenta cerrada o cancelada, la reactivacion falla")
        void sobre_una_cuenta_no_abierta_falla() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");
        }

        @Test
        @DisplayName("y el abono se queda apagado, asi que el saldo de la cuenta no baja")
        void el_saldo_de_la_cuenta_no_baja() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(IllegalStateException.class);

            // La invariante contable de changeStatus —saldo cero al cerrar— solo se
            // comprueba al cerrar y nunca despues: un abono que vuelve sobre una cuenta
            // CLOSE la deja cerrada con un saldo que ya nadie va a mover.
            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(refresher, cashPort);
        }
    }

    @Nested
    @DisplayName("el abono que vuelve no puede exceder el saldo pendiente")
    class Sobrepago {

        @Test
        @DisplayName("un abono mayor que el saldo pendiente no vuelve")
        void un_abono_mayor_que_el_saldo_pendiente_no_vuelve() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_ESTRECHO);

            // Entre la baja y la reactivacion entraron abonos nuevos legitimos: el viejo
            // ya no cabe. Sin esta guarda se sumaba encima y dejaba el saldo en negativo,
            // que es lo que denuncia el #218. Hoy sale el mismo mensaje que en el alta y
            // no el error opaco de OpenAccount.recalculate tres capas mas abajo.
            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no puede exceder el saldo pendiente");

            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(refresher, cashPort);
        }

        @Test
        @DisplayName("justo hasta el saldo pendiente si vuelve: la frontera es inclusiva")
        void justo_hasta_el_saldo_pendiente_si_vuelve() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(MONTO.toPlainString());
            elUpdateEnciendeLaFila(abono());

            // Dejar la cuenta en cero es saldarla, no sobrepagarla.
            assertThat(service.execute(comandoReactivar()).id()).isEqualTo(PAYMENT_ID);
        }
    }

    @Nested
    @DisplayName("la caja tiene que volver a recibir el ingreso")
    class Caja {

        @Test
        @DisplayName("el abono que vuelve entra otra vez en la caja del que reactiva")
        void el_abono_que_vuelve_entra_otra_vez_en_la_caja() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_PENDIENTE);
            elUpdateEnciendeLaFila(abono());

            service.execute(comandoReactivar());

            // Sin esto la cuenta daba el abono por cobrado y en el cajon no habia nada
            // que lo respaldara: el arqueo salia corto por el importe del abono y nadie
            // sabia de donde venia la diferencia.
            verify(cashPort).registerPayment(COMPANY_ID, OPEN_ACCOUNT_ID, PAYMENT_ID,
                    PaymentMethod.CASH, MONTO, OTRO_EMPLEADO.id());
        }

        @Test
        @DisplayName("sin caja abierta del actor no se enciende la fila")
        void sin_caja_abierta_del_actor_no_se_enciende_la_fila() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_PENDIENTE);
            doThrow(new IllegalStateException("no hay caja abierta")).when(cashPort)
                    .requireOpenSession(COMPANY_ID, OPEN_ACCOUNT_ID, OTRO_EMPLEADO.id());

            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(IllegalStateException.class);

            // La exigencia va ANTES del UPDATE, como en el alta: si el dinero no tiene
            // donde entrar, el abono no vuelve a contar como cobrado.
            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("un abono ANULADO vuelve a la vista sin tocar la caja")
        void un_abono_anulado_vuelve_a_la_vista_sin_tocar_la_caja() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitadoYAnulado());
            laCuentaSigueAbierta();
            elUpdateEnciendeLaFila(abonoAnulado());

            service.execute(comandoReactivar());

            // Un abono anulado no entra en la suma de abonos ni tiene ingreso vivo en
            // caja —se compenso al anularlo—: volver a registrarlo descuadraria el cajon
            // en el otro sentido. Ni siquiera se lee el saldo, porque no hay sobrepago
            // posible.
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            verify(openAccountQueryPort, never()).outstandingAmount(anyLong());
            verifyNoInteractions(cashPort);
        }

        @Test
        @DisplayName("un abono que YA estaba visible no se cobra dos veces ni se rechaza")
        void un_abono_ya_visible_no_se_cobra_dos_veces() {
            elAbonoApagadoCuelgaDeLaCuenta(abono());
            laCuentaSigueAbierta();
            elUpdateEnciendeLaFila(abono());

            assertThat(service.execute(comandoReactivar()).id()).isEqualTo(PAYMENT_ID);

            // Reactivar lo ya encendido es un no-op. Aplicarle el guard de sobrepago
            // seria un falso positivo —su importe YA esta descontado del saldo, asi que
            // el margen que queda es menor que el abono— y volver a registrarlo en caja,
            // un ingreso duplicado.
            verify(openAccountQueryPort, never()).outstandingAmount(anyLong());
            verifyNoInteractions(cashPort);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("un abono inexistente no bloquea nada")
        void un_abono_inexistente_no_bloquea_nada() {
            when(repository.lockAndFindOpenAccountIdIncludingDisabled(PAYMENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PAYMENT_ID));

            verifyNoInteractions(openAccountQueryPort, refresher, cashPort);
        }

        @Test
        @DisplayName("un abono de otra empresa se bloquea pero no se lee ni se reactiva")
        void un_abono_de_otra_empresa_no_se_reactiva() {
            // La lectura de bloqueo va SIN acotar por empresa a proposito (el JOIN
            // romperia el orden de los locks), asi que si resuelve la cuenta. Quien
            // convierte eso en un 404 con rollback es la carga acotada, que usa el mismo
            // EXISTS que el UPDATE.
            when(repository.lockAndFindOpenAccountIdIncludingDisabled(PAYMENT_ID))
                    .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
            when(repository.findByIdIncludingDisabledAndCompanyId(PAYMENT_ID, OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoReactivarDesdeOtraEmpresa()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class);

            // El lock de la cuenta tambien va acotado: sobre una cuenta ajena no bloquea
            // ninguna fila. Y no se llega a mirar su estado ni a encender nada.
            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, OTRA_COMPANY_ID);
            verify(openAccountQueryPort, never()).isOpen(anyLong());
            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(refresher, cashPort);
        }

        @Test
        @DisplayName("si el update no toco ninguna fila, alguien la borro entre medias")
        void si_no_reactivo_ninguna_fila_falla() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_PENDIENTE);
            when(repository.reactivate(PAYMENT_ID, COMPANY_ID)).thenReturn(0);

            // La carga que resolvio el abono usa el MISMO predicado que el UPDATE, asi
            // que un cero aqui ya no puede significar "es de otra empresa": solo un
            // borrado concurrente.
            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class);

            verifyNoInteractions(refresher);
            verify(cashPort, never()).registerPayment(anyLong(), anyLong(), anyLong(), any(), any(),
                    anyLong());
        }

        @Test
        @DisplayName("si el update dice que reactivo pero la recarga no lo encuentra, falla")
        void si_la_recarga_no_lo_encuentra_falla() {
            elAbonoApagadoCuelgaDeLaCuenta(abonoDeshabilitado());
            laCuentaSigueAbierta();
            saldoPendiente(SALDO_PENDIENTE);
            when(repository.reactivate(PAYMENT_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(PAYMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            // Carrera con un borrado concurrente: mejor fallar que refrescar la cuenta
            // con un abono que ya no esta.
            assertThatThrownBy(() -> service.execute(comandoReactivar()))
                    .isInstanceOf(DebtOpenAccountNotFoundException.class);

            verifyNoInteractions(refresher);
        }
    }
}
