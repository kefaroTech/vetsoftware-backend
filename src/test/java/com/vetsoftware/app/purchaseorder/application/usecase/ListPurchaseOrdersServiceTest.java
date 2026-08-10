package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPurchaseOrdersService — listados por empresa")
class ListPurchaseOrdersServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private ListPurchaseOrdersService service;

    @Test
    @DisplayName("proyecta a DTO cada orden activa de la empresa")
    void proyecta_cada_orden_activa() {
        when(repository.findAllByCompanyId(PurchaseOrderMother.COMPANY_ID))
                .thenReturn(List.of(PurchaseOrderMother.borrador(), PurchaseOrderMother.emitida()));

        List<PurchaseOrderDto> dtos = service.listByCompany(PurchaseOrderMother.COMPANY_ID);

        assertThat(dtos).hasSize(2).allSatisfy(dto -> assertThat(dto.enabled()).isTrue());
    }

    @Test
    @DisplayName("una empresa sin ordenes devuelve lista vacia, no null")
    void empresa_sin_ordenes_devuelve_lista_vacia() {
        when(repository.findAllByCompanyId(99L)).thenReturn(List.of());

        assertThat(service.listByCompany(99L)).isEmpty();
    }

    @Test
    @DisplayName("el listado de pausadas usa la consulta de deshabilitadas")
    void el_listado_de_pausadas_usa_su_propia_consulta() {
        when(repository.findAllDisabledByCompanyId(PurchaseOrderMother.COMPANY_ID))
                .thenReturn(List.of(PurchaseOrderMother.pausada()));

        List<PurchaseOrderDto> dtos = service.listDisabledByCompany(PurchaseOrderMother.COMPANY_ID);

        assertThat(dtos).singleElement().satisfies(dto -> {
            assertThat(dto.enabled()).isFalse();
            assertThat(dto.id()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("una empresa sin ordenes pausadas devuelve lista vacia")
    void empresa_sin_pausadas_devuelve_lista_vacia() {
        when(repository.findAllDisabledByCompanyId(99L)).thenReturn(List.of());

        assertThat(service.listDisabledByCompany(99L)).isEmpty();
    }
}
