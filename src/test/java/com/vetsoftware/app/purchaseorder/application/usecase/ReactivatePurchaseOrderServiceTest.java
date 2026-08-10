package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivatePurchaseOrderService — reactivacion de una orden pausada")
class ReactivatePurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private ReactivatePurchaseOrderService service;

    @Test
    @DisplayName("devuelve la orden releida despues de reactivarla")
    void devuelve_la_orden_releida() {
        when(repository.reactivate(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.borrador()));

        PurchaseOrderDto dto = service.execute(1L, PurchaseOrderMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("si la actualizacion no afecto filas la orden no era de la empresa")
    void sin_filas_afectadas_no_encuentra_la_orden() {
        when(repository.reactivate(1L, 99L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(1L, 99L))
                .isInstanceOf(PurchaseOrderNotFoundException.class)
                .hasMessageContaining("Purchase order not found: 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("si la relectura no encuentra la orden se reporta como inexistente")
    void relectura_vacia_reporta_no_encontrada() {
        when(repository.reactivate(1L, PurchaseOrderMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, PurchaseOrderMother.COMPANY_ID))
                .isInstanceOf(PurchaseOrderNotFoundException.class)
                .hasMessageContaining("Purchase order not found: 1");
    }
}
