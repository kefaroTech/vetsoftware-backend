package com.vetsoftware.app.tax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaTaxChildrenQueryPort")
class JpaTaxChildrenQueryPortTest {

    private static final Long TAX_ID = 500L;

    @Mock
    private ProductJpaRepository productJpaRepository;
    @Mock
    private ServiceJpaRepository serviceJpaRepository;

    @InjectMocks
    private JpaTaxChildrenQueryPort port;

    @Nested
    @DisplayName("existsActiveByTaxId")
    class ExistsActiveByTaxId {

        @Test
        @DisplayName("hay hijos activos si algun producto activo usa el impuesto")
        void hay_hijos_activos_por_producto() {
            when(productJpaRepository.existsByTax_Id(TAX_ID)).thenReturn(true);

            assertThat(port.existsActiveByTaxId(TAX_ID)).isTrue();
        }

        @Test
        @DisplayName("hay hijos activos si ningun producto lo usa pero si algun servicio")
        void hay_hijos_activos_por_servicio() {
            when(productJpaRepository.existsByTax_Id(TAX_ID)).thenReturn(false);
            when(serviceJpaRepository.existsByTax_Id(TAX_ID)).thenReturn(true);

            assertThat(port.existsActiveByTaxId(TAX_ID)).isTrue();
        }

        @Test
        @DisplayName("no hay hijos activos si ni productos ni servicios lo usan")
        void no_hay_hijos_activos() {
            when(productJpaRepository.existsByTax_Id(TAX_ID)).thenReturn(false);
            when(serviceJpaRepository.existsByTax_Id(TAX_ID)).thenReturn(false);

            assertThat(port.existsActiveByTaxId(TAX_ID)).isFalse();
        }
    }
}
