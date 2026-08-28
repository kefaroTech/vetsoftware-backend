package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaEntity;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaRepository;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileResponsibilityJpaEntity;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort.CompanyFiscalProfile;
import com.vetsoftware.app.electronicdocument.testsupport.ReflectionEntities;
import com.vetsoftware.app.withholdingconfig.infrastructure.persistence.WithholdingConfigJpaEntity;
import com.vetsoftware.app.withholdingconfig.infrastructure.persistence.WithholdingConfigJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyFiscalProfileQueryPort — arma el snapshot del emisor desde CompanyTaxProfile")
class JpaCompanyFiscalProfileQueryPortTest {

    @Mock
    private CompanyTaxProfileJpaRepository companyTaxProfileRepository;
    @Mock
    private WithholdingConfigJpaRepository withholdingConfigRepository;

    private JpaCompanyFiscalProfileQueryPort port;

    @BeforeEach
    void montar() {
        port = new JpaCompanyFiscalProfileQueryPort(companyTaxProfileRepository,
                withholdingConfigRepository);
    }

    private static CompanyTaxProfileJpaEntity perfil() throws Exception {
        CompanyTaxProfileJpaEntity profile = ReflectionEntities
                .newInstance(CompanyTaxProfileJpaEntity.class);
        profile.setCompanyDocumentType(CompanyDocumentType.NIT);
        profile.setCompanyDocumentId("900123456");
        profile.setCompanyDocumentVerificationDigit("7");
        profile.setLegalName("Veterinaria Vet SAS");
        profile.setTaxRegime(
                com.vetsoftware.app.companytaxprofile.domain.TaxRegime.RESPONSABLE_IVA);
        profile.setFiscalEmail("facturacion@vet.co");
        CompanyTaxProfileResponsibilityJpaEntity r1 = ReflectionEntities
                .newInstance(CompanyTaxProfileResponsibilityJpaEntity.class);
        r1.setCode("O-13");
        CompanyTaxProfileResponsibilityJpaEntity r2 = ReflectionEntities
                .newInstance(CompanyTaxProfileResponsibilityJpaEntity.class);
        r2.setCode("O-15");
        profile.setResponsibilities(List.of(r1, r2));
        return profile;
    }

    @Nested
    @DisplayName("empresa sin perfil fiscal")
    class SinPerfil {

        @Test
        @DisplayName("sin CompanyTaxProfile configurado devuelve vacio")
        void sin_perfil_devuelve_vacio() {
            when(companyTaxProfileRepository.findCurrentByCompanyId(9L))
                    .thenReturn(Optional.empty());

            assertThat(port.findByCompany(9L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("empresa con perfil fiscal")
    class ConPerfil {

        @Test
        @DisplayName("congela el emisor con sus responsabilidades fiscales")
        void congela_el_emisor_con_sus_responsabilidades() throws Exception {
            when(companyTaxProfileRepository.findCurrentByCompanyId(9L))
                    .thenReturn(Optional.of(perfil()));
            when(withholdingConfigRepository.findByCompany_Id(9L)).thenReturn(Optional.empty());

            Optional<CompanyFiscalProfile> result = port.findByCompany(9L);

            assertThat(result).isPresent();
            assertThat(result.get().issuer().legalName()).isEqualTo("Veterinaria Vet SAS");
            assertThat(result.get().issuer().documentId()).isEqualTo("900123456");
            assertThat(result.get().issuer().responsibilities()).containsExactly("O-13", "O-15");
        }

        @Test
        @DisplayName("perfil sin tipo de documento ni regimen tributario deja esos campos null en el emisor")
        void perfil_sin_tipo_documento_ni_regimen_deja_esos_campos_null() throws Exception {
            CompanyTaxProfileJpaEntity perfil = perfil();
            perfil.setCompanyDocumentType(null);
            perfil.setTaxRegime(null);
            when(companyTaxProfileRepository.findCurrentByCompanyId(9L))
                    .thenReturn(Optional.of(perfil));
            when(withholdingConfigRepository.findByCompany_Id(9L)).thenReturn(Optional.empty());

            Optional<CompanyFiscalProfile> result = port.findByCompany(9L);

            assertThat(result.get().issuer().documentType()).isNull();
            assertThat(result.get().issuer().taxRegime()).isNull();
        }

        @Test
        @DisplayName("sin WithholdingConfig, las tres tarifas de retencion quedan null")
        void sin_withholding_config_tarifas_nulas() throws Exception {
            when(companyTaxProfileRepository.findCurrentByCompanyId(9L))
                    .thenReturn(Optional.of(perfil()));
            when(withholdingConfigRepository.findByCompany_Id(9L)).thenReturn(Optional.empty());

            CompanyFiscalProfile profile = port.findByCompany(9L).orElseThrow();

            assertThat(profile.reteFuenteRate()).isNull();
            assertThat(profile.reteIvaRate()).isNull();
            assertThat(profile.reteIcaRate()).isNull();
        }

        @Test
        @DisplayName("con WithholdingConfig, trae las tres tarifas de retencion")
        void con_withholding_config_trae_las_tarifas() throws Exception {
            when(companyTaxProfileRepository.findCurrentByCompanyId(9L))
                    .thenReturn(Optional.of(perfil()));
            WithholdingConfigJpaEntity wc = ReflectionEntities
                    .newInstance(WithholdingConfigJpaEntity.class);
            wc.setReteFuenteRate(new BigDecimal("2.5"));
            wc.setReteIvaRate(new BigDecimal("15"));
            wc.setReteIcaRate(new BigDecimal("0.7"));
            when(withholdingConfigRepository.findByCompany_Id(9L)).thenReturn(Optional.of(wc));

            CompanyFiscalProfile profile = port.findByCompany(9L).orElseThrow();

            assertThat(profile.reteFuenteRate()).isEqualByComparingTo("2.5");
            assertThat(profile.reteIvaRate()).isEqualByComparingTo("15");
            assertThat(profile.reteIcaRate()).isEqualByComparingTo("0.7");
        }
    }
}
