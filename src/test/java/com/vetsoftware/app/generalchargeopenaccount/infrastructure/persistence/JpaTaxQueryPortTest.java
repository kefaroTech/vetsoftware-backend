package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother.IVA_19;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.domain.TaxScheme;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaTaxQueryPort (generalchargeopenaccount)")
class JpaTaxQueryPortTest {

    @Mock
    private TaxJpaRepository taxJpaRepository;

    @InjectMocks
    private JpaTaxQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea el impuesto encontrado en la empresa, con su esquema tributario")
        void mapea_el_impuesto_encontrado_con_su_esquema() {
            TaxJpaEntity entidad = mock(TaxJpaEntity.class);
            when(entidad.getId()).thenReturn(IVA_19.id());
            when(entidad.getName()).thenReturn(IVA_19.name());
            when(entidad.getPercentage()).thenReturn(IVA_19.percentage());
            when(entidad.getTaxScheme()).thenReturn(TaxScheme.IVA);
            when(taxJpaRepository.findByIdAndCompany_Id(IVA_19.id(), COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<TaxRef> ref = port.findById(IVA_19.id(), COMPANY_ID);

            assertThat(ref).contains(IVA_19);
        }

        @Test
        @DisplayName("un impuesto sin esquema tributario mapea el scheme como null")
        void un_impuesto_sin_esquema_mapea_null() {
            TaxJpaEntity entidad = mock(TaxJpaEntity.class);
            when(entidad.getId()).thenReturn(IVA_19.id());
            when(entidad.getName()).thenReturn(IVA_19.name());
            when(entidad.getPercentage()).thenReturn(IVA_19.percentage());
            when(entidad.getTaxScheme()).thenReturn(null);
            when(taxJpaRepository.findByIdAndCompany_Id(IVA_19.id(), COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<TaxRef> ref = port.findById(IVA_19.id(), COMPANY_ID);

            assertThat(ref).isPresent();
            assertThat(ref.orElseThrow().scheme()).isNull();
        }

        @Test
        @DisplayName("devuelve vacio si el impuesto no existe en esa empresa")
        void devuelve_vacio_si_no_existe_en_la_empresa() {
            when(taxJpaRepository.findByIdAndCompany_Id(IVA_19.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(IVA_19.id(), COMPANY_ID)).isEmpty();
        }
    }
}
