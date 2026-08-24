package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.LISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCatalogPricesService")
class ListCatalogPricesServiceTest {

    @Mock
    private CatalogPriceRepository repository;
    @Mock
    private PriceListRepository priceListRepository;
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    @InjectMocks
    private ListCatalogPricesService service;

    @Test
    @DisplayName("mapea el contenido y conserva los metadatos de la consulta")
    void conserva_los_metadatos() {
        when(priceListRepository.findById(LISTA))
                .thenReturn(Optional.of(PriceListMother.publicada()));
        PageResult<CatalogPrice> pagina = new PageResult<>(
                List.of(CatalogPriceMother.mensualGravado()), 0, 20, 4L, 1);
        when(repository.findAllByPriceListId(LISTA, 0, 20)).thenReturn(pagina);
        when(catalogItemQueryPort.findAllByIds(List.of(CatalogPriceMother.ARTICULO)))
                .thenReturn(Map.of());

        PageResult<CatalogPriceDto> resultado = service.listByPriceList(LISTA, 0, 20);

        assertThat(resultado.content()).singleElement().extracting(CatalogPriceDto::catalogItemId)
                .isEqualTo(CatalogPriceMother.ARTICULO);
        assertThat(resultado.totalElements()).isEqualTo(4L);
    }

    @Test
    @DisplayName("una lista inexistente no es una página vacía: son respuestas distintas")
    void lista_inexistente_no_es_pagina_vacia() {
        when(priceListRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByPriceList(9L, 0, 20))
                .isInstanceOf(PriceListNotFoundException.class);

        verify(repository, never()).findAllByPriceListId(anyLong(), anyInt(), anyInt());
    }
}
