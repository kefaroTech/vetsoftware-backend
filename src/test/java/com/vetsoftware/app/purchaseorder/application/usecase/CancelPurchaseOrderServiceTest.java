package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.InvalidPurchaseOrderStatusTransitionException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelPurchaseOrderService — cancelacion de la orden")
class CancelPurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private CancelPurchaseOrderService service;

    @Test
    @DisplayName("guarda la orden en CANCELLED sellando quien la cancelo")
    void guarda_la_orden_en_cancelled() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.emitida()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderDto dto = service.execute(1L, PurchaseOrderMother.COMPANY_ID, 55L);

        ArgumentCaptor<PurchaseOrder> guardado = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        assertThat(guardado.getValue().getUpdatedBy()).isEqualTo(55L);
        assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("una orden de otra empresa se comporta como inexistente y no escribe")
    void orden_de_otra_empresa_no_escribe() {
        when(repository.findByIdAndCompanyId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, 99L, 55L))
                .isInstanceOf(PurchaseOrderNotFoundException.class)
                .hasMessageContaining("Purchase order not found: 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("no se cancela una orden con mercancia ya recibida y no escribe")
    void orden_con_mercancia_recibida_no_se_cancela() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(
                Optional.of(PurchaseOrderMother.enEstado(PurchaseOrderStatus.PARTIALLY_RECEIVED,
                        List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10,
                                2)))));

        assertThatThrownBy(() -> service.execute(1L, PurchaseOrderMother.COMPANY_ID, 55L))
                .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                .hasMessageContaining("PARTIALLY_RECEIVED -> CANCELLED");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar una orden ya cancelada es un cambio de estado invalido")
    void orden_ya_cancelada_no_se_vuelve_a_cancelar() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(
                Optional.of(PurchaseOrderMother.enEstado(PurchaseOrderStatus.CANCELLED, List
                        .of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)))));

        assertThatThrownBy(() -> service.execute(1L, PurchaseOrderMother.COMPANY_ID, 55L))
                .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                .hasMessageContaining("CANCELLED -> CANCELLED");

        verify(repository, never()).save(any());
    }
}
