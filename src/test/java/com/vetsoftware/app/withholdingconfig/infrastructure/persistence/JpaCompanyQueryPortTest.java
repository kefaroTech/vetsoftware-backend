package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (withholdingconfig)")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;
    @InjectMocks
    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("company encontrada se mapea a CompanyRef")
    void company_encontrada_se_mapea_a_company_ref() {
        when(companyJpaRepository.findById(5L)).thenReturn(Optional.of(companyEntity));
        when(companyEntity.getId()).thenReturn(5L);
        when(companyEntity.getName()).thenReturn("Veterinaria Central");
        when(companyEntity.getIdentifier()).thenReturn("900123456-1");

        Optional<CompanyRef> ref = port.findById(5L);

        assertThat(ref).contains(new CompanyRef(5L, "Veterinaria Central", "900123456-1"));
    }

    @Test
    @DisplayName("company inexistente devuelve vacio")
    void company_inexistente_devuelve_vacio() {
        when(companyJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
