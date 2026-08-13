package com.vetsoftware.app.inventory.application.usecase;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COUNT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.EMPLEADO_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.OTRO_PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.TERCER_PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.comandoContar;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.conteo;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.conteoQueCuadra;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.saldo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.dto.InventoryCountLineView;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.PageResult;
import com.vetsoftware.app.inventory.application.port.in.StockLedgerUseCase;
import com.vetsoftware.app.inventory.application.port.out.BranchQueryPort;
import com.vetsoftware.app.inventory.application.port.out.InventoryCountRepository;
import com.vetsoftware.app.inventory.application.port.out.StockBalanceRepository;
import com.vetsoftware.app.inventory.domain.InventoryCount;
import com.vetsoftware.app.inventory.domain.InventoryCountNotFoundException;
import java.util.List;
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
@DisplayName("InventoryCountService — conteo fisico y los ajustes que genera")
class InventoryCountServiceTest {

    @Mock
    private StockLedgerUseCase stockLedger;
    @Mock
    private StockBalanceRepository balanceRepository;
    @Mock
    private InventoryCountRepository countRepository;
    @Mock
    private BranchQueryPort branchQueryPort;

    @InjectMocks
    private InventoryCountService service;

    @Captor
    private ArgumentCaptor<InventoryCount> conteoCaptor;
    @Captor
    private ArgumentCaptor<RecordAdjustmentCommand> ajusteCaptor;

    /** Sede valida y los tres saldos de sistema que el comando va a comparar. */
    private void sedeValidaConSaldos() {
        when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
        when(balanceRepository.find(PRODUCT_ID, BRANCH_ID)).thenReturn(Optional.of(saldo(10)));
        when(balanceRepository.find(OTRO_PRODUCT_ID, BRANCH_ID)).thenReturn(Optional.of(saldo(8)));
        when(balanceRepository.find(TERCER_PRODUCT_ID, BRANCH_ID))
                .thenReturn(Optional.of(saldo(5)));
    }

    @Nested
    @DisplayName("armado de la sesion")
    class Armado {

        @Test
        @DisplayName("congela el saldo de sistema de cada producto antes de conciliar")
        void congela_el_saldo_de_sistema_de_cada_producto() {
            sedeValidaConSaldos();
            when(countRepository.save(any())).thenReturn(conteo());

            service.record(comandoContar());

            verify(countRepository).save(conteoCaptor.capture());
            InventoryCount guardado = conteoCaptor.getValue();
            // El sistema lo pone el servicio leyendo el saldo, nunca el cliente: si
            // viniera del request, cualquiera podria fabricar la diferencia que quisiera.
            assertThat(guardado.getLines()).extracting(l -> l.getSystemQuantity())
                    .containsExactly(10, 8, 5);
            assertThat(guardado.getLines()).extracting(l -> l.getCountedQuantity())
                    .containsExactly(13, 6, 5);
        }

        @Test
        @DisplayName("un producto sin saldo en la sede cuenta como cero de sistema")
        void un_producto_sin_saldo_cuenta_como_cero() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            when(balanceRepository.find(PRODUCT_ID, BRANCH_ID)).thenReturn(Optional.empty());
            when(balanceRepository.find(OTRO_PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(8)));
            when(balanceRepository.find(TERCER_PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(5)));
            when(countRepository.save(any())).thenReturn(conteo());

            service.record(comandoContar());

            // Producto que nunca entro a esa sede: contar 13 significa que sobran 13, no
            // que el conteo falle.
            verify(countRepository).save(conteoCaptor.capture());
            assertThat(conteoCaptor.getValue().getLines().getFirst().getSystemQuantity()).isZero();
        }

        @Test
        @DisplayName("devuelve la vista con las lineas y sus diferencias")
        void devuelve_la_vista_con_las_diferencias() {
            sedeValidaConSaldos();
            when(countRepository.save(any())).thenReturn(conteo());

            InventoryCountView vista = service.record(comandoContar());

            assertThat(vista.id()).isEqualTo(COUNT_ID);
            assertThat(vista.totalLines()).isEqualTo(3);
            assertThat(vista.adjustedLines()).isEqualTo(2);
            assertThat(vista.lines()).extracting(InventoryCountLineView::difference)
                    .containsExactly(3, -2, 0);
        }
    }

    @Nested
    @DisplayName("ajustes generados")
    class Ajustes {

        @Test
        @DisplayName("genera un ajuste por linea con diferencia y ninguno por la que cuadra")
        void genera_un_ajuste_por_linea_con_diferencia() {
            sedeValidaConSaldos();
            when(countRepository.save(any())).thenReturn(conteo());

            service.record(comandoContar());

            // Tres lineas contadas, dos con diferencia: la que cuadra no puede ensuciar
            // el kardex con un movimiento de cero.
            verify(stockLedger, times(2)).recordAdjustment(ajusteCaptor.capture());
            assertThat(ajusteCaptor.getAllValues()).extracting(RecordAdjustmentCommand::delta)
                    .containsExactly(3, -2);
            assertThat(ajusteCaptor.getAllValues()).extracting(RecordAdjustmentCommand::productId)
                    .containsExactly(PRODUCT_ID, OTRO_PRODUCT_ID);
        }

        @Test
        @DisplayName("el ajuste referencia la sesion para poder trazarlo en el kardex")
        void el_ajuste_referencia_la_sesion() {
            sedeValidaConSaldos();
            when(countRepository.save(any())).thenReturn(conteo());

            service.record(comandoContar());

            verify(stockLedger, times(2)).recordAdjustment(ajusteCaptor.capture());
            RecordAdjustmentCommand primero = ajusteCaptor.getAllValues().getFirst();
            // Sin la referencia, un ajuste de conteo es indistinguible de uno manual y
            // se pierde el "quien conto y cuando" que justifica el movimiento.
            assertThat(primero.referenceId()).isEqualTo(COUNT_ID);
            assertThat(primero.reason()).isEqualTo("Conteo físico #" + COUNT_ID);
            assertThat(primero.createdBy()).isEqualTo(EMPLEADO_ID);
            assertThat(primero.branchId()).isEqualTo(BRANCH_ID);
            assertThat(primero.unitCost()).as("el costo lo resuelve el ledger").isNull();
        }

        @Test
        @DisplayName("un conteo que cuadra entero no toca el kardex")
        void un_conteo_que_cuadra_entero_no_toca_el_kardex() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(true);
            when(balanceRepository.find(PRODUCT_ID, BRANCH_ID)).thenReturn(Optional.of(saldo(10)));
            when(balanceRepository.find(OTRO_PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(8)));
            when(balanceRepository.find(TERCER_PRODUCT_ID, BRANCH_ID))
                    .thenReturn(Optional.of(saldo(5)));
            when(countRepository.save(any())).thenReturn(conteoQueCuadra());

            service.record(comandoContar());

            // La sesion se guarda igual (es rastro de auditoria), pero sin movimientos.
            verify(countRepository).save(any());
            verifyNoInteractions(stockLedger);
        }
    }

    @Nested
    @DisplayName("caminos de error: nada se escribe")
    class CaminosDeError {

        @Test
        @DisplayName("sede de otra empresa o inactiva")
        void sede_de_otra_empresa_o_inactiva() {
            when(branchQueryPort.existsActiveInCompany(BRANCH_ID, COMPANY_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.record(comandoContar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sede no válida o inactiva: " + BRANCH_ID);

            // Ni siquiera se leen los saldos: un conteo contra una sede ajena no puede
            // llegar a tocar el kardex de nadie.
            verify(countRepository, never()).save(any());
            verifyNoInteractions(stockLedger, balanceRepository);
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("el listado devuelve la pagina del repositorio")
        void el_listado_devuelve_la_pagina_del_repositorio() {
            SearchCountsQuery consulta = new SearchCountsQuery(COMPANY_ID, BRANCH_ID, 1, 20);
            when(countRepository.search(consulta)).thenReturn(new PageResult<>(List.of(
                    InventoryCountView.summary(COUNT_ID, BRANCH_ID, null, EMPLEADO_ID, null, 3, 2)),
                    1, 20, 41L, 3));

            PageResult<InventoryCountView> pagina = service.list(consulta);

            assertThat(pagina.content()).extracting(InventoryCountView::id)
                    .containsExactly(COUNT_ID);
            assertThat(pagina.totalElements()).isEqualTo(41L);
        }

        @Test
        @DisplayName("el detalle devuelve la sesion con sus lineas")
        void el_detalle_devuelve_la_sesion_con_sus_lineas() {
            when(countRepository.findDetail(COMPANY_ID, COUNT_ID))
                    .thenReturn(Optional.of(InventoryCountView.from(conteo())));

            InventoryCountView vista = service.get(COMPANY_ID, COUNT_ID);

            assertThat(vista.lines()).hasSize(3);
        }

        @Test
        @DisplayName("una sesion de otra empresa se ve como inexistente")
        void una_sesion_de_otra_empresa_se_ve_como_inexistente() {
            when(countRepository.findDetail(COMPANY_ID, COUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(COMPANY_ID, COUNT_ID))
                    .isInstanceOf(InventoryCountNotFoundException.class)
                    .hasMessageContaining(String.valueOf(COUNT_ID));
        }
    }
}
