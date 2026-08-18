package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.ANIMAL;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CHARGE_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CUENTA;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OTRA_CUENTA;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OTRA_CUENTA_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.SERVICIO;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.comandoActualizar;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.comandoTrasladar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
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
@DisplayName("UpdateServiceChargeOpenAccountService")
class UpdateServiceChargeOpenAccountServiceTest {

    @Mock
    private ServiceChargeOpenAccountRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ServiceQueryPort serviceQueryPort;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private UpdateServiceChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<ServiceChargeOpenAccount> cargoCaptor;

    private void referenciasResueltas() {
        when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                .thenReturn(Optional.of(ANIMAL));
        when(serviceQueryPort.findByIdAndCompanyId(SERVICIO.id(), COMPANY_ID))
                .thenReturn(Optional.of(SERVICIO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("aplica las referencias nuevas sobre el cargo cargado")
        void aplica_las_referencias_nuevas() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            referenciasResueltas();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(repository).save(cargoCaptor.capture());
            ServiceChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getAnimal()).isEqualTo(ANIMAL);
            assertThat(guardado.getService()).isEqualTo(SERVICIO);
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
        }

        @Test
        @DisplayName("no recalcula el precio congelado")
        void no_recalcula_el_precio_congelado() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            referenciasResueltas();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(repository).save(cargoCaptor.capture());
            assertThat(cargoCaptor.getValue().getTotalAmount()).isEqualByComparingTo("11900.00");
        }

        @Test
        @DisplayName("refresca solo la cuenta destino cuando el cargo no se mueve")
        void refresca_solo_la_cuenta_destino_cuando_no_se_mueve() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            referenciasResueltas();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoActualizar());

            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
            verify(refresher, never()).refresh(COMPANY_ID, OTRA_CUENTA_ID);
        }
    }

    @Nested
    @DisplayName("traslado entre cuentas")
    class Traslado {

        @Test
        @DisplayName("refresca las DOS cuentas: la vieja tambien cambia de total")
        void refresca_las_dos_cuentas() {
            // El cargo vive hoy en CUENTA (50) y el comando lo mueve a OTRA_CUENTA (51).
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OTRA_CUENTA_ID, COMPANY_ID))
                    .thenReturn(Optional.of(OTRA_CUENTA));
            referenciasResueltas();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoTrasladar());

            // Sin el refresh de la cuenta de origen, esa cuenta sigue mostrando un total
            // que ya incluye un cargo que se llevaron a otro sitio.
            verify(refresher).refresh(COMPANY_ID, OTRA_CUENTA_ID);
            verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("cargo inexistente o de otra empresa")
        void cargo_inexistente_o_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(ServiceChargeOpenAccountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CHARGE_ID));

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, animalQueryPort, serviceQueryPort);
        }

        @Test
        @DisplayName("cuenta destino inexistente")
        void cuenta_destino_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("cuenta destino de otra empresa: el cargo no se mueve a otro tenant")
        void cuenta_destino_de_otra_empresa() {
            // La cuenta destino existe pero es de otra empresa: la consulta acotada no la
            // resuelve y el traslado se rechaza. Antes la cuenta ajena SI se cargaba y
            // solo un if posterior impedia colgarle el importe.
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
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
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + ANIMAL.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("servicio inexistente")
        void servicio_inexistente() {
            when(repository.findByIdAndCompanyId(CHARGE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(cargo()));
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.of(ANIMAL));
            when(serviceQueryPort.findByIdAndCompanyId(SERVICIO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Service not found: " + SERVICIO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
