package com.vetsoftware.app.owner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort — resolucion del companion VO de company")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("compania existente se resuelve en un CompanyRef con sus tres campos")
    void compania_existente_se_resuelve_en_un_company_ref() {
        port = new JpaCompanyQueryPort(companyJpaRepository);
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(companyEntity));
        when(companyEntity.getId()).thenReturn(9L);
        when(companyEntity.getName()).thenReturn("Clinica Norte");
        when(companyEntity.getIdentifier()).thenReturn("NIT-900123456");

        Optional<CompanyRef> ref = port.findById(9L);

        assertThat(ref).contains(new CompanyRef(9L, "Clinica Norte", "NIT-900123456"));
    }

    @Test
    @DisplayName("compania inexistente devuelve Optional vacio")
    void compania_inexistente_devuelve_optional_vacio() {
        port = new JpaCompanyQueryPort(companyJpaRepository);
        when(companyJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
