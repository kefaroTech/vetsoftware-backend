package com.vetsoftware.app.pricelist.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.pricelist.application.port.out.CatalogPriceRepository;
import com.vetsoftware.app.pricelist.domain.CatalogPriceNotFoundException;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCatalogPriceService")
class FindCatalogPriceServiceTest {

    @Mock
    private CatalogPriceRepository repository;
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;

    @InjectMocks
    private FindCatalogPriceService service;

    @Test
    @DisplayName("proyecta el precio conservando el tratamiento fiscal exacto")
    void conserva_el_tratamiento_fiscal() {
        when(repository.findById(11L))
                .thenReturn(Optional.of(CatalogPriceMother.mensualExcluido()));
        when(catalogItemQueryPort.findById(CatalogPriceMother.ARTICULO))
                .thenReturn(Optional.empty());

        CatalogPriceDto dto = service.findById(11L);

        assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
        assertThat(dto.taxRate()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("un precio inexistente es un fallo explícito")
    void precio_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L))
                .isInstanceOf(CatalogPriceNotFoundException.class)
                .hasMessageContaining("Catalog price not found: 9");
    }
}
