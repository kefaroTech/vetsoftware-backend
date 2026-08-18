package com.vetsoftware.app.service.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaTaxQueryPort (service)")
class JpaTaxQueryPortTest {

    private static final Long TAX_ID = 30L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private TaxJpaRepository taxJpaRepository;

    @InjectMocks
    private JpaTaxQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea el impuesto encontrado, acotado a la empresa")
        void mapea_el_impuesto_encontrado() {
            TaxJpaEntity entidad = mock(TaxJpaEntity.class);
            when(entidad.getId()).thenReturn(TAX_ID);
            when(entidad.getName()).thenReturn("IVA 19%");
            when(entidad.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxJpaRepository.findByIdAndCompany_Id(TAX_ID, COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<TaxRef> ref = port.findById(TAX_ID, COMPANY_ID);

            assertThat(ref).contains(new TaxRef(TAX_ID, "IVA 19%", new BigDecimal("19.00")));
        }

        @Test
        @DisplayName("devuelve vacio si el impuesto no existe o es de otra empresa")
        void devuelve_vacio_si_no_existe_o_es_ajeno() {
            when(taxJpaRepository.findByIdAndCompany_Id(TAX_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(TAX_ID, COMPANY_ID)).isEmpty();
        }
    }
}
