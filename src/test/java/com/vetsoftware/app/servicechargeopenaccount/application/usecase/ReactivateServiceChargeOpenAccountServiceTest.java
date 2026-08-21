package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OTRA_COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateServiceChargeOpenAccountService")
class ReactivateServiceChargeOpenAccountServiceTest {

    @Mock
    private ServiceChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private OpenAccountRefresher refresher;

    @InjectMocks
    private ReactivateServiceChargeOpenAccountService service;

    /** El cargo deshabilitado existe y cuelga de una cuenta de esta empresa. */
    private void elCargoCuelgaDeLaCuenta() {
        when(repository.findOpenAccountIdIncludingDisabled(CHARGE_ID, COMPANY_ID))
                .thenReturn(Optional.of(OPEN_ACCOUNT_ID));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva, recarga el cargo y refresca el total de su cuenta")
        void reactiva_recarga_y_refresca() {
            elCargoCuelgaDeLaCuenta();
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
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
        @DisplayName("bloquea la cuenta ANTES de mirar su estado y de reactivar")
        void bloquea_la_cuenta_antes_de_mirar_su_estado() {
            elCargoCuelgaDeLaCuenta();
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(repository.reactivate(CHARGE_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));

            service.execute(CHARGE_ID, COMPANY_ID);

            // El orden es la garantia, no las llamadas sueltas: comprobar el estado sin
            // el lock tomado deja la ventana en la que la cuenta se cierra entre el
            // isOpen y el UPDATE, y el cargo revive igual sobre una cuenta cerrada.
            InOrder enOrden = inOrder(openAccountQueryPort, repository);
            enOrden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            enOrden.verify(openAccountQueryPort).isOpen(OPEN_ACCOUNT_ID);
            enOrden.verify(repository).reactivate(CHARGE_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("una cuenta que ya no esta abierta no admite cargos de vuelta (#239)")
    class CuentaNoAbierta {

        @Test
        @DisplayName("sobre una cuenta cerrada o cancelada, la reactivacion falla")
        void sobre_una_cuenta_no_abierta_falla() {
            elCargoCuelgaDeLaCuenta();
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(CHARGE_ID, COMPANY_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");
        }

        @Test
        @DisplayName("y el cargo se queda apagado, asi que el total de la cuenta no sube")
        void el_total_de_la_cuenta_no_sube() {
            elCargoCuelgaDeLaCuenta();
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(CHARGE_ID, COMPANY_ID))
                    .isInstanceOf(IllegalStateException.class);

            // El total se calcula sumando los cargos con enabled = true. Mientras el
            // UPDATE no se ejecute, este cargo sigue fuera de esa suma: por eso basta
            // con demostrar que no se reactivo. El refresh tampoco corre, asi que la
            // cuenta cerrada no se reescribe con un saldo pendiente que nadie podria
            // cobrar, que es exactamente lo que pasaba antes: en silencio y sin dejar
            // el saldo en negativo, donde la guarda de recalculate no lo veia.
            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(refresher);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("un cargo de otra empresa no resuelve cuenta y no se toca nada")
        void cargo_de_otra_empresa() {
            when(repository.findOpenAccountIdIncludingDisabled(CHARGE_ID, OTRA_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CHARGE_ID, OTRA_COMPANY_ID))
                    .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CHARGE_ID));

            // Ni siquiera se pide el lock: la cuenta ajena no se bloquea nunca.
            verify(repository, never()).reactivate(anyLong(), anyLong());
            verifyNoInteractions(openAccountQueryPort, refresher);
        }

        @Test
        @DisplayName("si el update no toco ninguna fila, alguien la borro entre medias")
        void si_no_reactivo_ninguna_fila_falla() {
            elCargoCuelgaDeLaCuenta();
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(repository.reactivate(CHARGE_ID, COMPANY_ID)).thenReturn(0);

            // La consulta que resolvio la cuenta usa el MISMO predicado que el UPDATE,
            // asi que un cero aqui ya no puede significar que el cargo sea de otra
            // empresa: solo un borrado concurrente.
            assertThatThrownBy(() -> service.execute(CHARGE_ID, COMPANY_ID))
                    .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class);

            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("si el update dice que reactivo pero la recarga no lo encuentra, falla")
        void si_la_recarga_no_lo_encuentra_falla() {
            elCargoCuelgaDeLaCuenta();
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(repository.reactivate(CHARGE_ID, COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            // Carrera con un borrado concurrente: mejor fallar que refrescar la cuenta
            // con un cargo que ya no esta.
            assertThatThrownBy(() -> service.execute(CHARGE_ID, COMPANY_ID))
                    .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class);

            verifyNoInteractions(refresher);
        }
    }
}
