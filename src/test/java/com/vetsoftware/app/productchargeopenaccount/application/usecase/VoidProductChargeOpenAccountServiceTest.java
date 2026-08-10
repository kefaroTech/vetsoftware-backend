package com.vetsoftware.app.productchargeopenaccount.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.productchargeopenaccount.application.command.VoidProductChargeOpenAccountCommand;
import com.vetsoftware.app.productchargeopenaccount.application.dto.ProductChargeOpenAccountDto;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductChargeOpenAccountRepository;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountAlreadyVoidedException;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccountNotFoundException;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.math.BigDecimal;
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
@DisplayName("VoidProductChargeOpenAccountService")
class VoidProductChargeOpenAccountServiceTest {

    @Mock
    private ProductChargeOpenAccountRepository repository;
    @Mock
    private OpenAccountQueryPort openAccountQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private OpenAccountRefresher refresher;
    @Mock
    private OpenAccountVersionGuard versionGuard;
    @Mock
    private InventoryLedgerPort inventoryLedger;

    @InjectMocks
    private VoidProductChargeOpenAccountService service;

    @Captor
    private ArgumentCaptor<ProductChargeOpenAccount> cargoCaptor;

    private void cargoExistente(ProductChargeOpenAccount cargo) {
        when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.of(cargo));
    }

    private void cuentaAbiertaConSaldo(String saldo) {
        when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                .thenReturn(true);
        when(openAccountQueryPort.outstandingAmount(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                .thenReturn(new BigDecimal(saldo));
    }

    private void empleadoResuelto() {
        when(employeeQueryPort.findByIdAndCompanyId(
                ProductChargeOpenAccountMother.OTRO_EMPLEADO.id(),
                ProductChargeOpenAccountMother.COMPANY_ID))
                .thenReturn(Optional.of(ProductChargeOpenAccountMother.OTRO_EMPLEADO));
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("marca el cargo como anulado con autor y motivo, sin deshabilitarlo")
        void marca_el_cargo_como_anulado_con_autor_y_motivo() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            cuentaAbiertaConSaldo("50000.00");
            empleadoResuelto();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ProductChargeOpenAccountMother.comandoAnular());

            verify(repository).save(cargoCaptor.capture());
            ProductChargeOpenAccount guardado = cargoCaptor.getValue();
            assertThat(guardado.isVoided()).isTrue();
            assertThat(guardado.getVoidedBy())
                    .isEqualTo(ProductChargeOpenAccountMother.OTRO_EMPLEADO);
            assertThat(guardado.getVoidReason()).isEqualTo("Cobrado por error");
            assertThat(guardado.getVoidedAt()).isNotNull();
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO ya anulado")
        void devuelve_el_dto_ya_anulado() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            cuentaAbiertaConSaldo("50000.00");
            empleadoResuelto();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProductChargeOpenAccountDto dto = service
                    .execute(ProductChargeOpenAccountMother.comandoAnular());

            assertThat(dto.voided()).isTrue();
            assertThat(dto.voidedBy().id())
                    .isEqualTo(ProductChargeOpenAccountMother.OTRO_EMPLEADO.id());
            assertThat(dto.voidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("compensa el inventario descontado al crear el cargo")
        void compensa_el_inventario_descontado() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            cuentaAbiertaConSaldo("50000.00");
            empleadoResuelto();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ProductChargeOpenAccountMother.comandoAnular());

            verify(inventoryLedger).reverseSale(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.OTRO_EMPLEADO.id());
            verify(refresher).refresh(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
        }

        @Test
        @DisplayName("anular por el importe exacto del saldo pendiente esta permitido")
        void anular_por_el_importe_exacto_del_saldo() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            // Limite exacto: total del cargo == saldo pendiente. La regla es
            // "cargo > saldo" y este caso NO la cruza.
            cuentaAbiertaConSaldo("11900.00");
            empleadoResuelto();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProductChargeOpenAccountDto dto = service
                    .execute(ProductChargeOpenAccountMother.comandoAnular());

            assertThat(dto.voided()).isTrue();
        }

        @Test
        @DisplayName("valida la version esperada de la cuenta antes de anular")
        void valida_la_version_esperada() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            cuentaAbiertaConSaldo("50000.00");
            empleadoResuelto();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new VoidProductChargeOpenAccountCommand(
                    ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OTRO_EMPLEADO.id(), "Cobrado por error", 3L));

            verify(versionGuard).assertVersion(ProductChargeOpenAccountMother.COMPANY_ID,
                    ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID, 3L);
        }
    }

    @Nested
    @DisplayName("la anulacion se rechaza y no escribe nada")
    class Rechazos {

        @Test
        @DisplayName("cargo de otra empresa o inexistente")
        void cargo_inexistente() {
            when(repository.findByIdAndCompanyId(ProductChargeOpenAccountMother.CHARGE_ID,
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoAnular()))
                    .isInstanceOf(ProductChargeOpenAccountNotFoundException.class)
                    .hasMessageContaining("ProductChargeOpenAccount not found: "
                            + ProductChargeOpenAccountMother.CHARGE_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(openAccountQueryPort, employeeQueryPort, inventoryLedger,
                    refresher, versionGuard);
        }

        @Test
        @DisplayName("cargo cuya cuenta pertenece a otra empresa")
        void cargo_cuya_cuenta_es_de_otra_empresa() {
            cargoExistente(ProductChargeOpenAccountMother.cargoDeOtraEmpresa());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoAnular()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("product charge does not belong to company");

            verify(repository, never()).save(any());
            verifyNoInteractions(openAccountQueryPort, employeeQueryPort, inventoryLedger,
                    refresher, versionGuard);
        }

        @Test
        @DisplayName("cuenta que ya no esta abierta")
        void cuenta_que_ya_no_esta_abierta() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            when(openAccountQueryPort.isOpen(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID))
                    .thenReturn(false);

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoAnular()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("open account is not OPEN");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, inventoryLedger, refresher);
        }

        @Test
        @DisplayName("anular dejaria el saldo pendiente negativo")
        void anular_dejaria_el_saldo_negativo() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            // El cargo vale 11.900 y solo quedan 5.000 por pagar: hay abonos que ya lo
            // cubren.
            cuentaAbiertaConSaldo("5000.00");

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoAnular()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se puede anular el cargo");

            verify(repository, never()).save(any());
            verifyNoInteractions(employeeQueryPort, inventoryLedger, refresher);
        }

        @Test
        @DisplayName("empleado que anula inexistente")
        void empleado_inexistente() {
            cargoExistente(ProductChargeOpenAccountMother.cargo());
            cuentaAbiertaConSaldo("50000.00");
            when(employeeQueryPort.findByIdAndCompanyId(
                    ProductChargeOpenAccountMother.OTRO_EMPLEADO.id(),
                    ProductChargeOpenAccountMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoAnular()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: "
                            + ProductChargeOpenAccountMother.OTRO_EMPLEADO.id());

            verify(repository, never()).save(any());
            verifyNoInteractions(inventoryLedger, refresher);
        }

        @Test
        @DisplayName("un cargo ya anulado no se anula dos veces ni repone inventario dos veces")
        void un_cargo_ya_anulado_no_se_anula_dos_veces() {
            cargoExistente(ProductChargeOpenAccountMother.cargoAnulado());
            cuentaAbiertaConSaldo("50000.00");
            empleadoResuelto();

            assertThatThrownBy(
                    () -> service.execute(ProductChargeOpenAccountMother.comandoAnular()))
                    .isInstanceOf(ProductChargeOpenAccountAlreadyVoidedException.class)
                    .hasMessageContaining(
                            "already voided: " + ProductChargeOpenAccountMother.CHARGE_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(inventoryLedger);
            verify(refresher, never()).refresh(anyLong(), anyLong());
        }
    }
}
