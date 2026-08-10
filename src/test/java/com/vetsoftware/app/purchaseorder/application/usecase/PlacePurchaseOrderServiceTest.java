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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlacePurchaseOrderService — emision de la orden al proveedor")
class PlacePurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private PlacePurchaseOrderService service;

    @Test
    @DisplayName("guarda la orden en PLACED sellando quien la emitio")
    void guarda_la_orden_en_placed() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.borrador()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderDto dto = service.execute(1L, PurchaseOrderMother.COMPANY_ID, 55L);

        ArgumentCaptor<PurchaseOrder> guardado = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
        assertThat(guardado.getValue().getUpdatedBy()).isEqualTo(55L);
        assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.PLACED);
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
    @DisplayName("emitir una orden ya emitida es un cambio de estado invalido y no escribe")
    void emitir_una_orden_ya_emitida_no_escribe() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.emitida()));

        assertThatThrownBy(() -> service.execute(1L, PurchaseOrderMother.COMPANY_ID, 55L))
                .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                .hasMessageContaining("PLACED -> PLACED");

        verify(repository, never()).save(any());
    }
}
