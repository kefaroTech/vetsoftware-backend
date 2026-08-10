package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeletePurchaseOrderService — baja logica de la orden")
class DeletePurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private DeletePurchaseOrderService service;

    @Test
    @DisplayName("borra la orden por id despues de comprobar que es de la empresa")
    void borra_la_orden_de_la_empresa() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.borrador()));

        service.execute(1L, PurchaseOrderMother.COMPANY_ID);

        verify(repository).delete(1L);
    }

    @Test
    @DisplayName("una orden de otra empresa no se borra y no toca el repositorio de escritura")
    void orden_de_otra_empresa_no_se_borra() {
        when(repository.findByIdAndCompanyId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, 99L))
                .isInstanceOf(PurchaseOrderNotFoundException.class)
                .hasMessageContaining("Purchase order not found: 1");

        verify(repository, never()).delete(any());
        verify(repository, never()).save(any());
    }
}
