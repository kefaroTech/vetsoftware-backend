package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.dianprovider.domain.CompanyRef;
import com.vetsoftware.app.dianprovider.testsupport.DianProviderConfigMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (dianprovider)")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @InjectMocks
    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("empresa existente se mapea a CompanyRef")
    void empresa_existente_se_mapea_a_company_ref() {
        CompanyJpaEntity entity = mock(CompanyJpaEntity.class);
        when(entity.getId()).thenReturn(DianProviderConfigMother.COMPANY_ID);
        when(entity.getName()).thenReturn(DianProviderConfigMother.CLINICA.name());
        when(entity.getIdentifier()).thenReturn(DianProviderConfigMother.CLINICA.identifier());
        when(companyJpaRepository.findById(DianProviderConfigMother.COMPANY_ID))
                .thenReturn(Optional.of(entity));

        Optional<CompanyRef> ref = port.findById(DianProviderConfigMother.COMPANY_ID);

        assertThat(ref).contains(DianProviderConfigMother.CLINICA);
    }

    @Test
    @DisplayName("empresa inexistente devuelve vacio")
    void empresa_inexistente_devuelve_vacio() {
        when(companyJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(port.findById(999L)).isEmpty();
    }
}
