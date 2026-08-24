package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.LISTA;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListNotEditableException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCatalogPriceService")
class DeleteCatalogPriceServiceTest {

    @Mock
    private CatalogPriceRepository repository;
    @Mock
    private PriceListRepository priceListRepository;

    @InjectMocks
    private DeleteCatalogPriceService service;

    @Test
    @DisplayName("da de baja un precio de una lista en borrador")
    void da_de_baja_en_borrador() {
        when(repository.findById(10L)).thenReturn(Optional.of(CatalogPriceMother.mensualGravado()));
        when(priceListRepository.lockById(LISTA))
                .thenReturn(Optional.of(PriceListMother.borrador()));

        service.execute(10L);

        verify(repository).delete(10L);
    }

    @ParameterizedTest
    @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
    @DisplayName("borrar el precio de una lista publicada haría desaparecer lo que se ofreció (R9)")
    void no_se_borra_de_una_lista_publicada(PriceListStatus estado) {
        when(repository.findById(10L)).thenReturn(Optional.of(CatalogPriceMother.mensualGravado()));
        when(priceListRepository.lockById(LISTA))
                .thenReturn(Optional.of(PriceListMother.enEstado(estado)));

        assertThatThrownBy(() -> service.execute(10L))
                .isInstanceOf(PriceListNotEditableException.class);

        verify(repository, never()).delete(anyLong());
    }

    @Test
    @DisplayName("un precio inexistente no se borra")
    void precio_inexistente() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(10L))
                .isInstanceOf(CatalogPriceNotFoundException.class);

        verify(repository, never()).delete(anyLong());
    }

    @Test
    @DisplayName("un precio cuya lista no se resuelve no se borra a ciegas")
    void lista_inexistente() {
        when(repository.findById(10L)).thenReturn(Optional.of(CatalogPriceMother.mensualGravado()));
        when(priceListRepository.lockById(LISTA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(10L))
                .isInstanceOf(PriceListNotFoundException.class);

        verify(repository, never()).delete(anyLong());
    }
}
