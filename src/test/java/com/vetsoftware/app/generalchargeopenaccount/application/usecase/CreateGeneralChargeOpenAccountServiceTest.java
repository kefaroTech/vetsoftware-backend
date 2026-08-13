package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CUENTA;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.CUENTA_AJENA;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.EMPLEADO;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.IVA_19;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargo;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.cargoConClave;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.comandoCrear;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.comandoCrearSinImpuesto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
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
@DisplayName("CreateGeneralChargeOpenAccountService")
class CreateGeneralChargeOpenAccountServiceTest {

    @Mock
    private GeneralChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private TaxQueryPort taxQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;

    @InjectMocks
    private CreateGeneralChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<GeneralChargeOpenAccount> cargoCaptor;

    /**
     * Cuenta abierta, de la empresa correcta y con todas las referencias resueltas.
     */
    private void todoEnOrden() {
        when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
        when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
        when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));
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
            GeneralChargeOpenAccount guardado = cargoCaptor.getValue();
            // Las refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardado.getOpenAccount()).isEqualTo(CUENTA);
            assertThat(guardado.getCreatedBy()).isEqualTo(EMPLEADO);
            assertThat(guardado.getTax()).isEqualTo(IVA_19);
            assertThat(guardado.getName()).isEqualTo("Traslado en ambulancia");
            assertThat(guardado.getId()).as("el cargo nuevo no trae id").isNull();
        }

        @Test
        @DisplayName("calcula el total desde unitario x cantidad y congela el impuesto")
        void calcula_el_total_y_congela_el_impuesto() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrear());

            verify(repository).save(cargoCaptor.capture());
            GeneralChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(guardado.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(guardado.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(guardado.getTaxPercentage()).isEqualByComparingTo("19.00");
        }

        @Test
        @DisplayName("un comando sin impuesto no consulta el catalogo de impuestos")
        void un_comando_sin_impuesto_no_consulta_el_catalogo() {
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(employeeQueryPort.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(EMPLEADO));
            when(repository.save(any())).thenReturn(cargo());

            service.execute(comandoCrearSinImpuesto());

            verifyNoInteractions(taxQueryPort);
            verify(repository).save(cargoCaptor.capture());
            assertThat(cargoCaptor.getValue().isHasTax()).isFalse();
            assertThat(cargoCaptor.getValue().getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("devuelve el DTO del cargo persistido, con su id")
        void devuelve_el_dto_del_cargo_persistido() {
            todoEnOrden();
            when(repository.save(any())).thenReturn(cargo(777L));

            GeneralChargeOpenAccountDto dto = service.execute(comandoCrear());

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
            orden.verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID);
            orden.verify(openAccountQueryPort).findById(OPEN_ACCOUNT_ID);
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
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(cargoConClave("req-1")));

            GeneralChargeOpenAccountDto dto = service.execute(comandoCrear("req-1"));

            assertThat(dto.id()).isEqualTo(cargoConClave("req-1").getId());
            // Un reintento no puede cobrar dos veces ni mover el total de la cuenta.
            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard, taxQueryPort, employeeQueryPort);
        }

        @Test
        @DisplayName("aun asi bloquea la cuenta: el chequeo de la clave tiene que ser serio")
        void aun_asi_bloquea_la_cuenta() {
            when(repository.findByOpenAccountIdAndClientRequestId(OPEN_ACCOUNT_ID, "req-1"))
                    .thenReturn(Optional.of(cargoConClave("req-1")));

            service.execute(comandoCrear("req-1"));

            verify(openAccountQueryPort).lockForUpdate(OPEN_ACCOUNT_ID);
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
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OpenAccount not found: " + OPEN_ACCOUNT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher);
        }

        @Test
        @DisplayName("cuenta de otra empresa: no se filtra el cargo a un tenant ajeno")
        void cuenta_de_otra_empresa() {
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID))
                    .thenReturn(Optional.of(CUENTA_AJENA));

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("open account does not belong to company");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, taxQueryPort, employeeQueryPort);
        }

        @Test
        @DisplayName("impuesto inexistente en la empresa")
        void impuesto_inexistente() {
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tax not found: " + IVA_19.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(refresher, employeeQueryPort);
        }

        @Test
        @DisplayName("empleado inexistente")
        void empleado_inexistente() {
            when(openAccountQueryPort.findById(OPEN_ACCOUNT_ID)).thenReturn(Optional.of(CUENTA));
            when(openAccountQueryPort.isOpen(OPEN_ACCOUNT_ID)).thenReturn(true);
            when(taxQueryPort.findById(IVA_19.id(), COMPANY_ID)).thenReturn(Optional.of(IVA_19));
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
