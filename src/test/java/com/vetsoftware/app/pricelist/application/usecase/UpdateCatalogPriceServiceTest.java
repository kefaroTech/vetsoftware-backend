package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.ARTICULO;
import static com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother.LISTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.command.UpdateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import com.vetsoftware.app.pricelist.domain.CatalogPriceTierOverlapException;
import com.vetsoftware.app.pricelist.domain.PriceListNotEditableException;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import com.vetsoftware.app.pricelist.testsupport.PriceListMother;
import java.math.BigDecimal;
import java.util.List;
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
@DisplayName("UpdateCatalogPriceService")
class UpdateCatalogPriceServiceTest {

    @Mock
    private CatalogPriceRepository repository;
    @Mock
    private PriceListRepository priceListRepository;

    // Solo para el resumen del articulo de la respuesta (#379). @InjectMocks lo
    // dejaria a null y el caso de uso reventaria con NPE al construir el DTO.
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    @InjectMocks
    private UpdateCatalogPriceService service;

    private static UpdateCatalogPriceCommand comando(int tierMin, Integer tierMax) {
        return new UpdateCatalogPriceCommand(10L, BillingCycle.MONTHLY, tierMin, tierMax, 2,
                new BigDecimal("9000.00"), new BigDecimal("0.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED);
    }

    @Test
    @DisplayName("aplica los cambios sobre un precio de una lista en borrador")
    void aplica_los_cambios() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(CatalogPriceMother.conTramo(10L, 1, 10)));
        when(priceListRepository.lockById(LISTA))
                .thenReturn(Optional.of(PriceListMother.borrador()));
        when(repository.findTierScope(LISTA, ARTICULO, BillingCycle.MONTHLY))
                .thenReturn(List.of(CatalogPriceMother.conTramo(10L, 1, 10)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogPriceDto dto = service.execute(comando(1, 20));

        assertThat(dto.tierMax()).isEqualTo(20);
        assertThat(dto.unitAmount()).isEqualTo(new BigDecimal("9000.00"));
    }

    @ParameterizedTest
    @EnumSource(value = PriceListStatus.class, names = {"PUBLISHED", "ARCHIVED"})
    @DisplayName("el precio de una lista publicada no se puede editar (R9)")
    void precio_de_lista_publicada_no_se_edita(PriceListStatus estado) {
        when(repository.findById(10L)).thenReturn(Optional.of(CatalogPriceMother.mensualGravado()));
        when(priceListRepository.lockById(LISTA))
                .thenReturn(Optional.of(PriceListMother.enEstado(estado)));

        assertThatThrownBy(() -> service.execute(comando(1, 20)))
                .isInstanceOf(PriceListNotEditableException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("mover un tramo encima de otro se rechaza")
    void mover_el_tramo_encima_de_otro_se_rechaza() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(CatalogPriceMother.conTramo(10L, 11, 20)));
        when(priceListRepository.lockById(LISTA))
                .thenReturn(Optional.of(PriceListMother.borrador()));
        when(repository.findTierScope(LISTA, ARTICULO, BillingCycle.MONTHLY)).thenReturn(List.of(
                CatalogPriceMother.conTramo(10L, 11, 20), CatalogPriceMother.conTramo(99L, 1, 10)));

        assertThatThrownBy(() -> service.execute(comando(5, 20)))
                .isInstanceOf(CatalogPriceTierOverlapException.class)
                .hasMessageContaining("overlaps catalog price 99");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un precio inexistente no se edita")
    void precio_inexistente() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando(1, 20)))
                .isInstanceOf(CatalogPriceNotFoundException.class)
                .hasMessageContaining("Catalog price not found: 10");

        verify(repository, never()).save(any());
    }
}
