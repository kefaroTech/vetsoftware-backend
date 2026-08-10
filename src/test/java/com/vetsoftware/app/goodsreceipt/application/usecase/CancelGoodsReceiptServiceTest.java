package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelGoodsReceiptService")
class CancelGoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository repository;
    @Mock
    private InventoryLedgerPort inventoryLedger;
    @Mock
    private PurchaseOrderReceivingPort purchaseOrderReceiving;

    @InjectMocks
    private CancelGoodsReceiptService service;

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

    private GoodsReceiptDto cancelar() {
        return service.execute(GoodsReceiptMother.RECEIPT_ID, GoodsReceiptMother.COMPANY_ID,
                GoodsReceiptMother.ACTOR_ID);
    }

    @Nested
    @DisplayName("Camino feliz sin orden de compra")
    class SinOrdenDeCompra {

        @Test
        @DisplayName("compensa el kardex de la recepcion completa por su id")
        void compensa_el_kardex() {
            recepcionExiste(GoodsReceiptMother.confirmada());
            devuelveLoQueSeGuarda();

            cancelar();

            verify(inventoryLedger).reverseReceipt(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.ACTOR_ID);
        }

        @Test
        @DisplayName("no toca la orden de compra cuando la recepcion era directa")
        void no_toca_la_orden_de_compra() {
            recepcionExiste(GoodsReceiptMother.confirmada());
            devuelveLoQueSeGuarda();

            cancelar();

            verifyNoInteractions(purchaseOrderReceiving);
        }

        @Test
        @DisplayName("guarda la recepcion en CANCELLED con el actor que la cancelo")
        void guarda_la_recepcion_cancelada() {
            recepcionExiste(GoodsReceiptMother.confirmada());
            devuelveLoQueSeGuarda();

            cancelar();

            verify(repository).save(receiptCaptor.capture());
            assertThat(receiptCaptor.getValue().getStatus())
                    .isEqualTo(GoodsReceiptStatus.CANCELLED);
            assertThat(receiptCaptor.getValue().getUpdatedBy())
                    .isEqualTo(GoodsReceiptMother.ACTOR_ID);
        }

        @Test
        @DisplayName("devuelve el DTO ya en CANCELLED")
        void devuelve_el_dto_cancelado() {
            recepcionExiste(GoodsReceiptMother.confirmada());
            devuelveLoQueSeGuarda();

            GoodsReceiptDto dto = cancelar();

            assertThat(dto.id()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(dto.status()).isEqualTo(GoodsReceiptStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Camino feliz con orden de compra")
    class ConOrdenDeCompra {

        @Test
        @DisplayName("revierte en la orden solo las lineas que enlazaban a una linea suya")
        void revierte_solo_las_lineas_enlazadas() {
            recepcionExiste(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.CONFIRMED));
            devuelveLoQueSeGuarda();

            cancelar();

            verify(purchaseOrderReceiving).revertReceipt(eq(GoodsReceiptMother.PURCHASE_ORDER_ID),
                    eq(GoodsReceiptMother.COMPANY_ID), receivedLinesCaptor.capture(),
                    eq(GoodsReceiptMother.ACTOR_ID));
            assertThat(receivedLinesCaptor.getValue()).containsExactly(new ReceivedLine(900L, 4));
        }

        @Test
        @DisplayName("compensa el kardex antes de tocar la orden de compra")
        void compensa_el_kardex_tambien() {
            recepcionExiste(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.CONFIRMED));
            devuelveLoQueSeGuarda();

            cancelar();

            verify(inventoryLedger).reverseReceipt(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.ACTOR_ID);
        }

        @Test
        @DisplayName("deja la recepcion en CANCELLED")
        void deja_la_recepcion_cancelada() {
            recepcionExiste(GoodsReceiptMother.conOrdenDeCompra(GoodsReceiptStatus.CONFIRMED));
            devuelveLoQueSeGuarda();

            assertThat(cancelar().status()).isEqualTo(GoodsReceiptStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Aislamiento por empresa y guarda de estado")
    class Guardas {

        @Test
        @DisplayName("no compensa nada si la recepcion es de otra empresa")
        void recepcion_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(GoodsReceiptMother.RECEIPT_ID,
                    GoodsReceiptMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cancelar()).isInstanceOf(GoodsReceiptNotFoundException.class)
                    .hasMessageContaining(
                            "Goods receipt not found: " + GoodsReceiptMother.RECEIPT_ID);

            verify(repository, never()).save(any());
            verifyNoInteractions(inventoryLedger, purchaseOrderReceiving);
        }

        @ParameterizedTest(name = "estado {0}")
        @EnumSource(value = GoodsReceiptStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("cancelar lo que no esta CONFIRMED no compensa el kardex")
        void cancelar_fuera_de_confirmada_no_toca_el_ledger(GoodsReceiptStatus status) {
            recepcionExiste(GoodsReceiptMother.conEstado(status));

            assertThatThrownBy(() -> cancelar())
                    .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class)
                    .hasMessageContaining("Cannot transition goods receipt from " + status)
                    .hasMessageContaining("to CANCELLED");

            verify(repository, never()).save(any());
            verifyNoInteractions(inventoryLedger, purchaseOrderReceiving);
        }
    }
}
