package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.ANIMAL;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.CUENTA;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.EMPLEADO;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.SERVICIO;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.cargoConClave;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.comandoCrear;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
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
@DisplayName("CreateServiceChargeOpenAccountService")
class CreateServiceChargeOpenAccountServiceTest {

    @Mock
    private ServiceChargeOpenAccountRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private ServiceQueryPort serviceQueryPort;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private CreateServiceChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<ServiceChargeOpenAccount> cargoCaptor;

    /**
     * Cuenta abierta, de la empresa correcta y con todas las referencias resueltas.
     */
    private void todoEnOrden() {
        when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(CUENTA));
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                .thenReturn(Optional.of(ANIMAL));
        when(serviceQueryPort.findByIdAndCompanyId(SERVICIO.id(), COMPANY_ID))
                .thenReturn(Optional.of(SERVICIO));
        when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID))
                .thenReturn(Optional.of(EMPLEADO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda el cargo con las referencias resueltas por los puertos")
        void guarda_el_cargo_con_las_referencias_resueltas() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrear());

            verify(repository).save(cargoCaptor.capture());
            ServiceChargeOpenAccount guardado = cargoCaptor.getValue();
            // Las refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardado.getAnimal()).isEqualTo(ANIMAL);
            assertThat(guardado.getService()).isEqualTo(SERVICIO);
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
            assertThat(guardado.getCreatedBy()).isEqualTo(EMPLEADO);
            assertThat(guardado.getId()).as("el cargo nuevo no trae id").isNull();
        }

        @Test
        @DisplayName("congela el precio del catalogo con su desglose de impuesto")
        void congela_el_precio_del_catalogo() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrear());

            verify(repository).save(cargoCaptor.capture());
            ServiceChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(guardado.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(guardado.getTaxAmount()).isEqualByComparingTo("1900.00");
        }

        @Test
        @DisplayName("devuelve el DTO del cargo persistido, con su id")
        void devuelve_el_dto_del_cargo_persistido() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo(777L));

            ServiceChargeOpenAccountDto dto = service.execute(comandoCrear());

            assertThat(dto.id()).isEqualTo(777L);
        }

        @Test
        @DisplayName("bloquea la cuenta antes de leerla y refresca su total al terminar")
        void bloquea_la_cuenta_antes_de_leerla_y_refresca_al_terminar() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrear());

            // El lock tiene que ir ANTES de la lectura: si se toma despues, dos cargos
            // concurrentes leen el mismo saldo y el total de la cuenta queda mal.
            InOrder orden = Mockito.inOrder(openAccountQueryPort, repository, refresher);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(repository).save(any());
            orden.verify(refresher).refresh(COMPANY_ID, OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("comprueba la version esperada de la cuenta")
        void comprueba_la_version_esperada_de_la_cuenta() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrear());

            verify(versionGuard).assertVersion(COMPANY_ID, OPEN_ACCOUNT_ID, null);
        }
    }

    @Nested
    @DisplayName("idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("con una clave ya usada devuelve el cargo anterior sin volver a guardar")
        void con_una_clave_ya_usada_devuelve_el_cargo_anterior() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(cargoConClave("req-1")));

            ServiceChargeOpenAccountDto dto = service.execute(comandoCrear("req-1"));

            // El reintento legitimo del mismo cliente sigue devolviendo EL MISMO cargo:
            // acotar la via de idempotencia no puede costar la idempotencia.
            assertThat(dto.id()).isEqualTo(cargoConClave("req-1").getId());
            // Un reintento no puede cobrar dos veces ni mover el total de la cuenta.
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, animalQueryPort, serviceQueryPort,
                    employeeQueryPort);
        }

        @Test
        @DisplayName("un reintento con la clave de otra empresa no devuelve su cargo")
        void un_reintento_con_la_clave_de_otra_empresa_no_devuelve_su_cargo() {
            // La cuenta del comando es de otro tenant, asi que la resolucion acotada no
            // la encuentra. Con el chequeo de idempotencia por delante —como estaba— el
            // finder no lleva empresa (el cargo no tiene company_id propio) y bastaba
            // acertar el clientRequestId para que el servicio devolviera el DTO del cargo
            // ajeno sin pasar por ninguna comprobacion.
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear("req-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).findByOpenAccountIdAndClientRequestId(any(), any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, animalQueryPort, serviceQueryPort,
                    employeeQueryPort);
        }

        @Test
        @DisplayName("bloquea y resuelve la cuenta ANTES de mirar la clave de idempotencia")
        void bloquea_y_resuelve_la_cuenta_antes_de_mirar_la_clave() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(cargoConClave("req-1")));

            service.execute(comandoCrear("req-1"));

            // El lock sigue siendo la primera sentencia (el reintento que llega segundo
            // tiene que leer el cargo ya committeado por el rival) y la resolucion acotada
            // va antes del finder: es lo unico que demuestra que la cuenta es de esta
            // empresa antes de leer sus cargos.
            InOrder orden = Mockito.inOrder(openAccountQueryPort, repository);
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(openAccountQueryPort).findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID);
            orden.verify(repository).findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID,
                    "req-1");
            verifyNoInteractions(versionGuard);
        }

        @Test
        @DisplayName("con una clave nueva sigue el camino normal")
        void con_una_clave_nueva_sigue_el_camino_normal() {
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-nueva"))
                    .thenReturn(Optional.empty());
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrear("req-nueva"));

            verify(repository).save(cargoCaptor.capture());
            assertThat(cargoCaptor.getValue().getClientRequestId()).isEqualTo("req-nueva");
        }

        @Test
        @DisplayName("una clave en blanco no activa la deduplicacion")
        void una_clave_en_blanco_no_activa_la_deduplicacion() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

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
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("cuenta de otra empresa: no se carga contra un tenant ajeno")
        void cuenta_de_otra_empresa() {
            // La cuenta existe, pero es de otra empresa: la consulta acotada no la
            // resuelve, asi que el cargo se rechaza igual que si no existiera. Antes la
            // cuenta ajena SI se cargaba y solo un if posterior evitaba el cargo.
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            // Si el servicio volviera a la variante ancha, la cuenta ajena entraria de
            // nuevo en el flujo: el importe quedaria en la cuenta del otro tenant.
            verify(openAccountQueryPort, never()).findById(any());
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
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
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("animal inexistente")
        void animal_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + ANIMAL.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("servicio inexistente")
        void servicio_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.of(ANIMAL));
            when(serviceQueryPort.findByIdAndCompanyId(SERVICIO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Service not found: " + SERVICIO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("empleado inexistente")
        void empleado_inexistente() {
            when(openAccountQueryPort.findByIdAndCompanyId(OPEN_ACCOUNT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(animalQueryPort.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.of(ANIMAL));
            when(serviceQueryPort.findByIdAndCompanyId(SERVICIO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(SERVICIO));
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + EMPLEADO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }
    }
}
