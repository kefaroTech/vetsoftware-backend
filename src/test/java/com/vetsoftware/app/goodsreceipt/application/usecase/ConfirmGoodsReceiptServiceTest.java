package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.PurchaseOrderReceivingPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.ReceivedLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La transicion DRAFT→CONFIRMED es la unica guarda de idempotencia frente al
 * kardex: {@code recordReceipt} no lo es. Por eso los escenarios de "ya
 * confirmada" afirman CERO interacciones con el ledger, no solo la excepcion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmGoodsReceiptService")
class ConfirmGoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository repository;
    @Mock
    private InventoryLedgerPort inventoryLedger;
    @Mock
    private PurchaseOrderReceivingPort purchaseOrderReceiving;

    @InjectMocks
    private ConfirmGoodsReceiptService service;

    @Captor
    private ArgumentCaptor<GoodsReceipt> receiptCaptor;
    @Captor
    private ArgumentCaptor<List<ReceivedLine>> receivedLinesCaptor;

    private void recepcionExiste(GoodsReceipt receipt) {
        when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.of(receipt));
    }

    private void devuelveLoQueSeGuarda() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private GoodsReceiptDto confirmar() {
        return service.execute(GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.COMPANY_ID,
                GoodsReceiptMother.ACTOR_ID);
    }

    @Nested
    @DisplayName("Camino feliz sin orden de compra")
    class SinOrdenDeCompra {

        @Test
        @DisplayName("registra la entrada de kardex de la linea con lote, costo y sede")
        void registra_la_entrada_de_kardex() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            devuelveLoQueSeGuarda();

            confirmar();

            verify(inventoryLedger).recordReceipt(GoodsReceiptMother.COMPANY_ID,
                    GoodsReceiptMother.SEDE.id(), GoodsReceiptMother.VACUNA.id(), "LOTE-A",
                    GoodsReceiptMother.VENCIMIENTO, 10, GoodsReceiptMother.COSTO,
                    GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.ACTOR_ID);
        }

        @Test
        @DisplayName("registra una entrada por cada linea de la recepcion")
        void registra_una_entrada_por_linea() {
            recepcionExiste(GoodsReceiptMother.conDosLineas(GoodsReceiptStatus.DRAFT));
            devuelveLoQueSeGuarda();

            confirmar();

            verify(inventoryLedger, times(2)).recordReceipt(anyLong(), anyLong(), anyLong(),
                    anyString(), any(), anyInt(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("no toca la orden de compra cuando la recepcion es directa")
        void no_toca_la_orden_de_compra() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            devuelveLoQueSeGuarda();

            confirmar();

            verifyNoInteractions(purchaseOrderReceiving);
        }

        @Test
        @DisplayName("guarda la recepcion en CONFIRMED con el actor que la confirmo")
        void guarda_la_recepcion_confirmada() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            devuelveLoQueSeGuarda();

            confirmar();

            verify(repository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getStatus())
                    .isEqualTo(GoodsReceiptStatus.CONFIRMED);
            assertThat(receiptCaptor.getValue().getUpdatedBy())
                    .isEqualTo(GoodsReceiptMother.ACTOR_ID);
        }

        @Test
        @DisplayName("devuelve el DTO ya en CONFIRMED")
        void devuelve_el_dto_confirmado() {
            recepcionExiste(GoodsReceiptMother.enBorrador());
            devuelveLoQueSeGuarda();

            GoodsReceiptDto dto = confirmar();

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(dto.status()).isEqualTo(GoodsReceiptStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Camino feliz con orden de compra")
    class ConOrdenDeCompra {

        @Test
        @DisplayName("aplica en la orden solo las lineas que enlazan a una linea de la orden")
        void aplica_solo_las_lineas_enlazadas() {
            recepcionExiste(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.DRAFT));
            devuelveLoQueSeGuarda();

            confirmar();

            verify(purchaseOrderReceiving).applyReceipt(eq(GoodsReceiptMother.PURCHASE_ORDER_ID),
                    eq(GoodsReceiptMother.COMPANY_ID), receivedLinesCaptor.capture(),
                    eq(GoodsReceiptMother.ACTOR_ID));
            assertThat(receivedLinesCaptor.getValue()).containsExactly(new ReceivedLine(900L, 4));
        }

        @Test
        @DisplayName("registra el kardex de todas las lineas, tambien las no enlazadas")
        void registra_el_kardex_de_todas_las_lineas() {
            recepcionExiste(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.DRAFT));
            devuelveLoQueSeGuarda();

            confirmar();

            verify(inventoryLedger, times(2)).recordReceipt(anyLong(), anyLong(), anyLong(),
                    anyString(), any(), anyInt(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("deja la recepcion en CONFIRMED igual que la recepcion directa")
        void deja_la_recepcion_confirmada() {
            recepcionExiste(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.DRAFT));
            devuelveLoQueSeGuarda();

            assertThat(confirmar().status()).isEqualTo(GoodsReceiptStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Aislamiento por empresa y guarda de idempotencia")
    class Guardas {

        @Test
        @DisplayName("no toca inventario ni orden si la recepcion es de otra empresa")
        void recepcion_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> confirmar()).isInstanceOf(GoodsReceiptNotFoundException.class)
                    .hasMessageContaining(
                            "Goods receipt not found: " + GoodsReceiptMother.RECEIPT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(inventoryLedger, purchaseOrderReceiving);
        }

        @ParameterizedTest(name = "estado {0}")
        @EnumSource(value = GoodsReceiptStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("confirmar dos veces no duplica el kardex: cero interacciones con el ledger")
        void confirmar_fuera_de_borrador_no_toca_el_ledger(GoodsReceiptStatus status) {
            recepcionExiste(GoodsReceiptMother.conEstado(status));

            assertThatThrownBy(() -> confirmar())
                    .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class)
                    .hasMessageContaining("Cannot transition goods receipt from " + status)
                    .hasMessageContaining("to CONFIRMED");

            verify(repository, never()).save(any());
            verifyNoInteractions(inventoryLedger, purchaseOrderReceiving);
        }
    }
}
