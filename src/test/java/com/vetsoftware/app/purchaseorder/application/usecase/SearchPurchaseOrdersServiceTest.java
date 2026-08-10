package com.vetsoftware.app.purchaseorder.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PageResult;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchPurchaseOrdersService — busqueda paginada")
class SearchPurchaseOrdersServiceTest {

    @Mock
    private PurchaseOrderRepository repository;
    @InjectMocks
    private SearchPurchaseOrdersService service;

    private static SearchPurchaseOrdersCommand comando() {
        return new SearchPurchaseOrdersCommand(PurchaseOrderMother.COMPANY_ID, 7L, 4L,
                PurchaseOrderStatus.PLACED, PurchaseOrderMother.FECHA_ORDEN,
                PurchaseOrderMother.FECHA_ESPERADA, 1, 10);
    }

    @Test
    @DisplayName("mapea el contenido a DTO conservando los metadatos de la pagina")
    void mapea_contenido_conservando_metadatos() {
        PageResult<PurchaseOrder> pagina = new PageResult<>(List.of(PurchaseOrderMother.emitida()),
                1, 10, 21L, 3);
        when(repository.search(comando())).thenReturn(pagina);

        PageResult<PurchaseOrderDto> resultado = service.execute(comando());

        assertThat(resultado.content()).singleElement()
                .satisfies(dto -> assertThat(dto.status()).isEqualTo(PurchaseOrderStatus.PLACED));
        assertThat(resultado.page()).isEqualTo(1);
        assertThat(resultado.pageSize()).isEqualTo(10);
        assertThat(resultado.totalElements()).isEqualTo(21L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("una busqueda sin resultados devuelve una pagina vacia con sus contadores en cero")
    void busqueda_sin_resultados_devuelve_pagina_vacia() {
        SearchPurchaseOrdersCommand sinFiltros = new SearchPurchaseOrdersCommand(
                PurchaseOrderMother.COMPANY_ID, null, null, null, null, null, 0, 20);
        when(repository.search(sinFiltros)).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<PurchaseOrderDto> resultado = service.execute(sinFiltros);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
    }
}
