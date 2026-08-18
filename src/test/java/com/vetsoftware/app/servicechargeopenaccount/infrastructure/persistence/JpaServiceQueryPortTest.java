package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import com.vetsoftware.app.tax.domain.TaxScheme;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
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
@DisplayName("JpaServiceQueryPort (servicechargeopenaccount)")
class JpaServiceQueryPortTest {

    private static final Long SERVICE_ID = 2L;

    @Mock
    private ServiceJpaRepository serviceJpaRepository;
    @Mock
    private ServiceJpaEntity serviceEntity;
    @Mock
    private TaxJpaEntity taxEntity;

    @InjectMocks
    private JpaServiceQueryPort port;

    private void servicioBase(TaxTreatment treatment) {
        when(serviceEntity.getId()).thenReturn(SERVICE_ID);
        when(serviceEntity.getName()).thenReturn("Consulta general");
        when(serviceEntity.getPrice()).thenReturn(new BigDecimal("11900"));
        when(serviceEntity.getTaxTreatment()).thenReturn(treatment);
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("un servicio de otra empresa no aparece")
        void un_servicio_de_otra_empresa_no_aparece() {
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("GRAVADO con impuesto vigente arrastra el TaxRef completo")
        void gravado_con_impuesto_vigente_arrastra_el_tax_ref() {
            servicioBase(TaxTreatment.GRAVADO);
            when(serviceEntity.getTax()).thenReturn(taxEntity);
            when(taxEntity.getId()).thenReturn(4L);
            when(taxEntity.getName()).thenReturn("IVA 19%");
            when(taxEntity.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxEntity.getTaxScheme()).thenReturn(TaxScheme.IVA);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            assertThat(ref.hasTax()).isTrue();
            assertThat(ref.tax().id()).isEqualTo(4L);
            assertThat(ref.tax().scheme()).isEqualTo("IVA");
            assertThat(ref.taxTreatment()).isEqualTo("GRAVADO");
        }

        @Test
        @DisplayName("INC tambien cuenta como gravado")
        void inc_tambien_cuenta_como_gravado() {
            servicioBase(TaxTreatment.INC);
            when(serviceEntity.getTax()).thenReturn(taxEntity);
            when(taxEntity.getId()).thenReturn(6L);
            when(taxEntity.getName()).thenReturn("INC 8%");
            when(taxEntity.getPercentage()).thenReturn(new BigDecimal("8.00"));
            when(taxEntity.getTaxScheme()).thenReturn(TaxScheme.INC);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            assertThat(ref.hasTax()).isTrue();
            assertThat(ref.tax().scheme()).isEqualTo("INC");
        }

        @Test
        @DisplayName("GRAVADO sin impuesto asociado en el catalogo no genera TaxRef")
        void gravado_sin_impuesto_asociado_no_genera_tax_ref() {
            servicioBase(TaxTreatment.GRAVADO);
            when(serviceEntity.getTax()).thenReturn(null);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            // hasTax lo decide solo el tratamiento; el ref se cae a null si el catalogo no
            // tiene el impuesto enlazado, y eso no puede tumbar el mapeo.
            assertThat(ref.hasTax()).isTrue();
            assertThat(ref.tax()).isNull();
        }

        @Test
        @DisplayName("EXENTO no es gravado pero conserva el tratamiento congelado")
        void exento_no_es_gravado_pero_conserva_el_tratamiento() {
            servicioBase(TaxTreatment.EXENTO);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            assertThat(ref.hasTax()).isFalse();
            assertThat(ref.tax()).isNull();
            assertThat(ref.taxTreatment()).isEqualTo("EXENTO");
        }

        @Test
        @DisplayName("EXCLUIDO tampoco es gravado")
        void excluido_tampoco_es_gravado() {
            servicioBase(TaxTreatment.EXCLUIDO);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            assertThat(ref.hasTax()).isFalse();
            assertThat(ref.taxTreatment()).isEqualTo("EXCLUIDO");
        }

        @Test
        @DisplayName("un tax sin esquema en el catalogo viaja con scheme null")
        void un_tax_sin_esquema_viaja_con_scheme_null() {
            servicioBase(TaxTreatment.GRAVADO);
            when(serviceEntity.getTax()).thenReturn(taxEntity);
            when(taxEntity.getId()).thenReturn(4L);
            when(taxEntity.getName()).thenReturn("Sin esquema");
            when(taxEntity.getPercentage()).thenReturn(BigDecimal.ZERO);
            when(taxEntity.getTaxScheme()).thenReturn(null);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            assertThat(ref.tax().scheme()).isNull();
        }

        @Test
        @DisplayName("un servicio sin tratamiento tributario viaja con taxTreatment null")
        void un_servicio_sin_tratamiento_viaja_con_null() {
            servicioBase(null);
            when(serviceJpaRepository.findByIdAndCompany_Id(SERVICE_ID, COMPANY_ID))
                    .thenReturn(Optional.of(serviceEntity));

            ServiceRef ref = port.findByIdAndCompanyId(SERVICE_ID, COMPANY_ID).orElseThrow();

            assertThat(ref.hasTax()).isFalse();
            assertThat(ref.taxTreatment()).isNull();
        }
    }
}
