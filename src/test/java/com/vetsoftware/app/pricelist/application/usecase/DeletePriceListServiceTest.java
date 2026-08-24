package com.vetsoftware.app.pricelist.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceListHasActivePricesException;
import com.vetsoftware.app.pricelist.domain.PriceListNotEditableException;
import com.vetsoftware.app.pricelist.domain.PriceListNotFoundException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
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
@DisplayName("DeletePriceListService")
class DeletePriceListServiceTest {

    @Mock
    private PriceListRepository repository;
    @Mock
    private CatalogPriceRepository catalogPriceRepository;

    @InjectMocks
    private DeletePriceListService service;

    @Test
    @DisplayName("da de baja un borrador vacío")
    void da_de_baja_un_borrador_vacio() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.borrador()));
        when(catalogPriceRepository.countByPriceListId(1L)).thenReturn(0L);

        service.execute(1L);

        verify(repository).delete(1L);
    }

    @ParameterizedTest
    @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
    @DisplayName("una lista publicada no se da de baja: se archiva (R9)")
    void publicada_no_se_da_de_baja(PriceListStatus estado) {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.enEstado(estado)));

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(PriceListNotEditableException.class);

        verify(repository, never()).delete(anyLong());
        verifyNoInteractions(catalogPriceRepository);
    }

    @Test
    @DisplayName("un borrador con precios activos no se da de baja: quedarían huérfanos")
    void borrador_con_precios_no_se_da_de_baja() {
        when(repository.findById(1L)).thenReturn(Optional.of(PriceListMother.borrador()));
        when(catalogPriceRepository.countByPriceListId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.execute(1L)).isInstanceOfSatisfying(
                PriceListHasActivePricesException.class, ex -> org.assertj.core.api.Assertions
                        .assertThat(ex.getActivePrices()).isEqualTo(3L));

        verify(repository, never()).delete(anyLong());
    }

    @Test
    @DisplayName("una lista inexistente no se da de baja")
    void lista_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(9L))
                .isInstanceOf(PriceListNotFoundException.class);

        verify(repository, never()).delete(anyLong());
    }
}
