package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindPurchaseOrderService — detalle de una orden")
class FindPurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private FindPurchaseOrderService service;

    @Test
    @DisplayName("devuelve el DTO completo de la orden de la empresa")
    void devuelve_el_dto_completo() {
        when(repository.findByIdAndCompanyId(1L, PurchaseOrderMother.COMPANY_ID))
                .thenReturn(Optional.of(PurchaseOrderMother.emitidaConDosLineas()));

        PurchaseOrderDto dto = service.findById(1L, PurchaseOrderMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.PLACED);
        assertThat(dto.lines()).hasSize(2);
        assertThat(dto.supplier().name()).isEqualTo("Distribuidora Animal");
    }

    @Test
    @DisplayName("una orden de otra empresa se comporta como inexistente")
    void orden_de_otra_empresa_no_se_encuentra() {
        when(repository.findByIdAndCompanyId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(PurchaseOrderNotFoundException.class)
                .hasMessageContaining("Purchase order not found: 1");
    }
}
