package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.command.ApplyReceiptCommand;
import com.vetsoftware.app.purchaseorder.application.command.ReceivedPurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceivePurchaseOrderService — aplicar y revertir recepciones de mercancia")
class ReceivePurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private ReceivePurchaseOrderService service;

    private static ApplyReceiptCommand comando(ReceivedPurchaseOrderLine... lineas) {
        return new ApplyReceiptCommand(1L, PurchaseOrderMother.COMPANY_ID, List.of(lineas), 55L);
    }

    private ArgumentCaptor<PurchaseOrder> capturaGuardado() {
        ArgumentCaptor<PurchaseOrder> guardado = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(repository).save(guardado.capture());
        return guardado;
    }

    @Nested
    @DisplayName("Aplicar recepcion")
    class Aplicar {

        @Test
        @DisplayName("recibir todo en todas las lineas guarda la orden en RECEIVED")
        void recibir_todo_guarda_en_received() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(PurchaseOrderMother.emitidaConDosLineas()));

            service.applyReceipt(comando(new ReceivedPurchaseOrderLine(100L, 10),
                    new ReceivedPurchaseOrderLine(200L, 4)));

            PurchaseOrder guardado = capturaGuardado().getValue();
            assertThat(guardado.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(guardado.getLines())
                    .extracting(PurchaseOrderLine::getId, PurchaseOrderLine::getQuantityReceived)
                    .containsExactly(tuple(100L, 10), tuple(200L, 4));
            assertThat(guardado.getUpdatedBy()).isEqualTo(55L);
        }

        @Test
        @DisplayName("recibir parte de una sola linea guarda la orden en PARTIALLY_RECEIVED")
        void recibir_parte_guarda_en_partially_received() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(PurchaseOrderMother.emitidaConDosLineas()));

            service.applyReceipt(comando(new ReceivedPurchaseOrderLine(100L, 6)));

            PurchaseOrder guardado = capturaGuardado().getValue();
            assertThat(guardado.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
            assertThat(guardado.getLines().get(0).getQuantityReceived()).isEqualTo(6);
            assertThat(guardado.getLines().get(1).getQuantityReceived()).isZero();
        }

        @Test
        @DisplayName("dos recepciones sucesivas acumulan hasta cerrar la linea")
        void dos_recepciones_acumulan_hasta_cerrar() {
            PurchaseOrder order = PurchaseOrderMother.emitida();
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(order));

            service.applyReceipt(comando(new ReceivedPurchaseOrderLine(100L, 4)));
            service.applyReceipt(comando(new ReceivedPurchaseOrderLine(100L, 6)));

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(order.getLines().get(0).getQuantityReceived()).isEqualTo(10);
        }

        @Test
        @DisplayName("recibir mas de lo pendiente se rechaza y no escribe")
        void recibir_mas_de_lo_pendiente_no_escribe() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(PurchaseOrderMother.emitida()));

            assertThatThrownBy(
                    () -> service.applyReceipt(comando(new ReceivedPurchaseOrderLine(100L, 11))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds pending quantity 10");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una linea que no pertenece a la orden se rechaza y no escribe")
        void linea_ajena_no_escribe() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(PurchaseOrderMother.emitida()));

            assertThatThrownBy(
                    () -> service.applyReceipt(comando(new ReceivedPurchaseOrderLine(999L, 1))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Purchase order line 999 not found in order 1");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una orden de otra empresa se comporta como inexistente y no escribe")
        void orden_de_otra_empresa_no_escribe() {
            when(repository.findByIdAndCompanyId(1L, 99L)).thenReturn(Optional.empty());

            ApplyReceiptCommand ajeno = new ApplyReceiptCommand(1L, 99L,
                    List.of(new ReceivedPurchaseOrderLine(100L, 1)), 55L);

            assertThatThrownBy(() -> service.applyReceipt(ajeno))
                    .isInstanceOf(PurchaseOrderNotFoundException.class)
                    .hasMessageContaining("Purchase order not found: 1");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una recepcion sin lineas solo recalcula y no altera lo recibido")
        void recepcion_sin_lineas_solo_recalcula() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                    .thenReturn(Optional.of(PurchaseOrderMother.emitida()));

            service.applyReceipt(comando());

            PurchaseOrder guardado = capturaGuardado().getValue();
            assertThat(guardado.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
            assertThat(guardado.getLines().get(0).getQuantityReceived()).isZero();
        }
    }

    @Nested
    @DisplayName("Revertir recepcion")
    class Revertir {

        @Test
        @DisplayName("revertir parcialmente deja la orden en PARTIALLY_RECEIVED")
        void revertir_parte_deja_partially_received() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(
                    Optional.of(PurchaseOrderMother.enEstado(PurchaseOrderStatus.RECEIVED, List.of(
                            PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 10)))));

            service.revertReceipt(comando(new ReceivedPurchaseOrderLine(100L, 3)));

            PurchaseOrder guardado = capturaGuardado().getValue();
            assertThat(guardado.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
            assertThat(guardado.getLines().get(0).getQuantityReceived()).isEqualTo(7);
        }

        @Test
        @DisplayName("revertir todo lo recibido devuelve la orden a PLACED")
        void revertir_todo_devuelve_a_placed() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(
                    Optional.of(PurchaseOrderMother.enEstado(PurchaseOrderStatus.RECEIVED, List.of(
                            PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 10)))));

            service.revertReceipt(comando(new ReceivedPurchaseOrderLine(100L, 10)));

            PurchaseOrder guardado = capturaGuardado().getValue();
            assertThat(guardado.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
            assertThat(guardado.getLines().get(0).getQuantityReceived()).isZero();
        }

        @Test
        @DisplayName("revertir mas de lo recibido no deja la cantidad en negativo")
        void revertir_de_mas_no_deja_negativo() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(
                    Optional.of(PurchaseOrderMother.enEstado(PurchaseOrderStatus.PARTIALLY_RECEIVED,
                            List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10,
                                    2)))));

            service.revertReceipt(comando(new ReceivedPurchaseOrderLine(100L, 50)));

            PurchaseOrder guardado = capturaGuardado().getValue();
            assertThat(guardado.getLines().get(0).getQuantityReceived()).isZero();
            assertThat(guardado.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
        }

        @Test
        @DisplayName("una orden de otra empresa se comporta como inexistente y no escribe")
        void orden_de_otra_empresa_no_escribe() {
            when(repository.findByIdAndCompanyId(1L, 99L)).thenReturn(Optional.empty());

            ApplyReceiptCommand ajeno = new ApplyReceiptCommand(1L, 99L,
                    List.of(new ReceivedPurchaseOrderLine(100L, 1)), 55L);

            assertThatThrownBy(() -> service.revertReceipt(ajeno))
                    .isInstanceOf(PurchaseOrderNotFoundException.class)
                    .hasMessageContaining("Purchase order not found: 1");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("revertir una cantidad no positiva se rechaza y no escribe")
        void revertir_cantidad_no_positiva_no_escribe() {
            when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(
                    Optional.of(PurchaseOrderMother.enEstado(PurchaseOrderStatus.RECEIVED, List.of(
                            PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 10)))));

            assertThatThrownBy(
                    () -> service.revertReceipt(comando(new ReceivedPurchaseOrderLine(100L, 0))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reverted quantity must be greater than zero");

            verify(repository, never()).save(any());
        }
    }
}
