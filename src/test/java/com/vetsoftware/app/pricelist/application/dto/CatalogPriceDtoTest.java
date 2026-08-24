package com.vetsoftware.app.pricelist.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogPriceDto")
class CatalogPriceDtoTest {

    @Test
    @DisplayName("copia campo por campo, sin recalcular ningún importe")
    void copia_campo_por_campo() {
        CatalogPrice precio = CatalogPriceMother.conTramo(10L, 3, 10);

        CatalogPriceDto dto = CatalogPriceDto.from(precio);

        assertThat(dto.id()).isEqualTo(precio.getId());
        assertThat(dto.priceListId()).isEqualTo(precio.getPriceListId());
        assertThat(dto.catalogItemId()).isEqualTo(precio.getCatalogItemId());
        assertThat(dto.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(dto.tierMin()).isEqualTo(3);
        assertThat(dto.tierMax()).isEqualTo(10);
        assertThat(dto.includedQuantity()).isEqualTo(precio.getIncludedQuantity());
        assertThat(dto.unitAmount()).isEqualTo(precio.getUnitAmount());
        assertThat(dto.setupAmount()).isEqualTo(precio.getSetupAmount());
        assertThat(dto.taxRate()).isEqualTo(precio.getTaxRate());
        assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
        assertThat(dto.createdDate()).isEqualTo(precio.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un precio excluido sale como EXCLUDED, no como tarifa cero a secas")
    void el_excluido_sale_como_excluido() {
        CatalogPriceDto dto = CatalogPriceDto.from(CatalogPriceMother.mensualExcluido());

        assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
        assertThat(dto.taxRate()).isEqualByComparingTo("0.00");
    }
}
