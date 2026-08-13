package com.vetsoftware.app.inventory.application.usecase;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COSTO;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.EMPLEADO_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.OTRA_BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.saldo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.SetMinStockCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.application.port.out.BranchQueryPort;
import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.domain.StockBalance;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
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

/**
 * El servicio administrativo no toca el kardex: valida la sede y delega en el
 * ledger. Lo que estos tests fijan es justo eso — que la validacion de sede
 * pase SIEMPRE antes del movimiento, y que el comando llegue al ledger sin
 * reinterpretar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryAdminService — validacion de sede y delegacion al ledger")
class InventoryAdminServiceTest {

    @Mock
    private StockLedgerUseCase stockLedger;
    @Mock
    private StockBalanceRepository balanceRepository;
    @Mock
    private BranchQueryPort branchQueryPort;

    @InjectMocks
    private InventoryAdminService service;

    @Captor
    private ArgumentCaptor<StockBalance> saldoCaptor;

    private static final RecordAdjustmentCommand AJUSTE = new RecordAdjustmentCommand(COMPANY_ID,
            BRANCH_ID, PRODUCT_ID, 3, COSTO, "Sobrante de conteo", 500L, EMPLEADO_ID);

    private static final RecordPurchaseCommand COMPRA = new RecordPurchaseCommand(COMPANY_ID,
            BRANCH_ID, PRODUCT_ID, "L-2026-01", null, 10, COSTO, StockReferenceType.GOODS_RECEIPT,
            900L, EMPLEADO_ID);

    private static final RecordClinicalUseCommand CONSUMO = new RecordClinicalUseCommand(COMPANY_ID,
            BRANCH_ID, PRODUCT_ID, 2, 800L, "Cirugia", EMPLEADO_ID);

    private static final TransferStockCommand TRASLADO = new TransferStockCommand(COMPANY_ID,
            BRANCH_ID, OTRA_BRANCH_ID, PRODUCT_ID, 5, "Reposicion", EMPLEADO_ID);

    private void sedeValida() {
        when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("delegacion al ledger")
    class Delegacion {

        @Test
        @DisplayName("el ajuste llega al ledger tal cual")
        void el_ajuste_llega_al_ledger_tal_cual() {
            sedeValida();

            service.adjust(AJUSTE);

            verify(stockLedger).recordAdjustment(AJUSTE);
        }

        @Test
        @DisplayName("la entrada de mercancia llega al ledger tal cual")
        void la_entrada_llega_al_ledger_tal_cual() {
            sedeValida();

            service.receive(COMPRA);

            verify(stockLedger).recordPurchase(COMPRA);
        }

        @Test
        @DisplayName("el consumo clinico llega al ledger tal cual")
        void el_consumo_llega_al_ledger_tal_cual() {
            sedeValida();

            service.consume(CONSUMO);

            verify(stockLedger).recordClinicalUse(CONSUMO);
        }
    }

    @Nested
    @DisplayName("traslado entre sedes")
    class Traslado {

        @Test
        @DisplayName("valida las DOS sedes antes de mover nada")
        void valida_las_dos_sedes() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            when(branchQueryPort.existsActiveInCompany(OTRA_BRANCH_ID, COMPANY_ID))
                    .thenReturn(true);

            service.transfer(TRASLADO);

            verify(branchQueryPort).existsActiveInCompany(BRANCH_ID, COMPANY_ID);
            verify(branchQueryPort).existsActiveInCompany(OTRA_BRANCH_ID, COMPANY_ID);
            verify(stockLedger).transfer(TRASLADO);
        }

        @Test
        @DisplayName("una sede destino ajena aborta el traslado")
        void una_sede_destino_ajena_aborta_el_traslado() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            when(branchQueryPort.existsActiveInCompany(OTRA_BRANCH_ID, COMPANY_ID))
                    .thenReturn(false);

            // Sin validar el destino, un traslado sacaria stock de una sede propia para
            // meterlo en la de otro tenant.
            assertThatThrownBy(() -> service.transfer(TRASLADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sede no válida o inactiva: " + OTRA_BRANCH_ID);

            verifyNoInteractions(stockLedger);
        }
    }

    @Nested
    @DisplayName("una sede invalida corta antes de tocar el kardex")
    class SedeInvalida {

        @Test
        @DisplayName("el ajuste no se registra")
        void el_ajuste_no_se_registra() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.adjust(AJUSTE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sede no válida o inactiva: " + BRANCH_ID);

            verifyNoInteractions(stockLedger);
        }

        @Test
        @DisplayName("la entrada no se registra")
        void la_entrada_no_se_registra() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.receive(COMPRA))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(stockLedger);
        }

        @Test
        @DisplayName("el consumo no se registra")
        void el_consumo_no_se_registra() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.consume(CONSUMO))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(stockLedger);
        }

        @Test
        @DisplayName("el minimo de stock no se guarda")
        void el_minimo_no_se_guarda() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service
                    .setMinStock(new SetMinStockCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 5)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(balanceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setMinStock")
    class MinimoDeStock {

        @Test
        @DisplayName("actualiza el minimo del saldo existente")
        void actualiza_el_minimo_del_saldo_existente() {
            sedeValida();
            when(balanceRepository.findForUpdate(PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(10)));

            service.setMinStock(new SetMinStockCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 8));

            verify(balanceRepository).save(saldoCaptor.capture());
            assertThat(saldoCaptor.getValue().getMinStock()).isEqualTo(8);
            // Cambiar el minimo no puede mover el stock: son dos cosas distintas.
            assertThat(saldoCaptor.getValue().getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("crea el saldo en cero si el producto todavia no tiene fila en la sede")
        void crea_el_saldo_si_no_existe() {
            sedeValida();
            when(balanceRepository.findForUpdate(PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.empty());

            service.setMinStock(new SetMinStockCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 8));

            // Poner el minimo antes de la primera compra es normal: el saldo nace en 0 y
            // la alerta de bajo stock ya queda armada.
            verify(balanceRepository).save(saldoCaptor.capture());
            assertThat(saldoCaptor.getValue().getQuantity()).isZero();
            assertThat(saldoCaptor.getValue().getMinStock()).isEqualTo(8);
            assertThat(saldoCaptor.getValue().getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(saldoCaptor.getValue().getBranchId()).isEqualTo(BRANCH_ID);
        }

        @Test
        @DisplayName("un minimo negativo se rechaza antes incluso de validar la sede")
        void un_minimo_negativo_se_rechaza_primero() {
            assertThatThrownBy(() -> service
                    .setMinStock(new SetMinStockCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID, -1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minStock cannot be negative");

            verifyNoInteractions(branchQueryPort, balanceRepository);
        }

        @Test
        @DisplayName("un minimo de cero es valido: desactiva la alerta")
        void un_minimo_de_cero_es_valido() {
            sedeValida();
            when(balanceRepository.findForUpdate(PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(10)));

            service.setMinStock(new SetMinStockCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 0));

            verify(balanceRepository).save(saldoCaptor.capture());
            assertThat(saldoCaptor.getValue().getMinStock()).isZero();
        }

        @Test
        @DisplayName("lee el saldo con lock: el minimo se toca a la vez que las salidas")
        void lee_el_saldo_con_lock() {
            sedeValida();
            when(balanceRepository.findForUpdate(PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(10)));

            service.setMinStock(new SetMinStockCommand(COMPANY_ID, BRANCH_ID, PRODUCT_ID, 8));

            // findForUpdate y no find: la misma fila la esta moviendo el ledger en cada
            // venta, y un update sin lock perderia la cantidad o el minimo.
            verify(balanceRepository).findForUpdate(PRODUCT_ID, BRANCH_ID);
            verify(balanceRepository, never()).find(any(), any());
        }
    }
}
