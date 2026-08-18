package com.vetsoftware.app.goodsreceipt.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchGoodsReceiptsService")
class SearchGoodsReceiptsServiceTest {

    @Mock
    private GoodsReceiptRepository repository;

    @InjectMocks
    private SearchGoodsReceiptsService service;

    @Nested
    @DisplayName("Busqueda paginada")
    class BusquedaPaginada {

        @Test
        @DisplayName("mapea la pagina de recepciones a DTOs conservando los metadatos")
        void mapea_la_pagina_conservando_metadatos() {
            PageResult<com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt> pagina = PageResult
                    .of(java.util.List.of(GoodsReceiptMother.enBorrador()), 0, 20, 1L);
            when(repository.search(GoodsReceiptMother.comandoBuscar())).thenReturn(pagina);

            PageResult<GoodsReceiptDto> resultado = service
                    .execute(GoodsReceiptMother.comandoBuscar());

            assertThat(resultado.content()).extracting(GoodsReceiptDto::id)
                    .containsExactly(GoodsReceiptMother.RECEIPT_ID);
            assertThat(resultado.totalElements()).isEqualTo(1L);
            assertThat(resultado.page()).isZero();
        }

        @Test
        @DisplayName("una busqueda sin resultados devuelve contenido vacio")
        void una_busqueda_sin_resultados_devuelve_contenido_vacio() {
            when(repository.search(GoodsReceiptMother.comandoBuscar()))
                    .thenReturn(PageResult.empty(0, 20));

            PageResult<GoodsReceiptDto> resultado = service
                    .execute(GoodsReceiptMother.comandoBuscar());

            assertThat(resultado.content()).isEmpty();
            assertThat(resultado.totalElements()).isZero();
        }
    }
}
