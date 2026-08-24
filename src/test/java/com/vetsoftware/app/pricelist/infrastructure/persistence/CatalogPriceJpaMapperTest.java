package com.vetsoftware.app.pricelist.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.pricelist.testsupport.CatalogPriceMother;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogPriceJpaMapper — ida y vuelta dominio/entidad")
class CatalogPriceJpaMapperTest {

    private final CatalogPriceJpaMapper mapper = new CatalogPriceJpaMapper();

    @Test
    @DisplayName("el viaje de ida y vuelta no pierde ni un campo")
    void ida_y_vuelta_sin_perdidas() {
        CatalogPrice original = CatalogPriceMother.conTramo(10L, 3, 10);

        CatalogPrice vuelta = mapper.toDomain(mapper.toJpa(original));

        assertThat(vuelta.getId()).isEqualTo(original.getId());
        assertThat(vuelta.getPriceListId()).isEqualTo(original.getPriceListId());
        assertThat(vuelta.getCatalogItemId()).isEqualTo(original.getCatalogItemId());
        assertThat(vuelta.getBillingCycle()).isEqualTo(original.getBillingCycle());
        assertThat(vuelta.getTierMin()).isEqualTo(original.getTierMin());
        assertThat(vuelta.getTierMax()).isEqualTo(original.getTierMax());
        assertThat(vuelta.getIncludedQuantity()).isEqualTo(original.getIncludedQuantity());
        assertThat(vuelta.getUnitAmount()).isEqualTo(original.getUnitAmount());
        assertThat(vuelta.getSetupAmount()).isEqualTo(original.getSetupAmount());
        assertThat(vuelta.getTaxRate()).isEqualTo(original.getTaxRate());
        assertThat(vuelta.getTaxTreatment()).isEqualTo(original.getTaxTreatment());
        assertThat(vuelta.getVersion()).isEqualTo(original.getVersion());
        assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
    }

    @Test
    @DisplayName("EXCLUDED no se convierte en EXEMPT por el camino")
    void el_tratamiento_fiscal_no_se_confunde() {
        CatalogPrice excluido = CatalogPriceMother.mensualExcluido();

        CatalogPriceJpaEntity entity = mapper.toJpa(excluido);

        assertThat(entity.getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
        assertThat(mapper.toDomain(entity).getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
    }

    @Test
    @DisplayName("los importes viajan con escala 2, la de la columna DECIMAL(19,2)")
    void los_importes_viajan_con_escala_dos() {
        CatalogPriceJpaEntity entity = mapper.toJpa(CatalogPriceMother.mensualGravado());

        assertThat(entity.getUnitAmount()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(entity.getUnitAmount().scale()).isEqualTo(2);
        assertThat(entity.getTaxRate().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("un tramo abierto viaja con tier_max nulo")
    void tramo_abierto_viaja_con_max_nulo() {
        CatalogPriceJpaEntity entity = mapper.toJpa(CatalogPriceMother.conTramo(10L, 11, null));

        assertThat(entity.getTierMax()).isNull();
        assertThat(mapper.toDomain(entity).getTierMax()).isNull();
    }
}
