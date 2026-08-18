package com.vetsoftware.app.permission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.permission.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (permission)")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    @InjectMocks
    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("empresa existente se mapea a CompanyRef")
    void empresa_existente_se_mapea() {
        when(companyEntity.getId()).thenReturn(9L);
        when(companyEntity.getName()).thenReturn("Clinica Norte");
        when(companyEntity.getIdentifier()).thenReturn("NIT-900");
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(companyEntity));

        Optional<CompanyRef> ref = port.findById(9L);

        assertThat(ref).contains(new CompanyRef(9L, "Clinica Norte", "NIT-900"));
    }

    @Test
    @DisplayName("empresa inexistente devuelve vacio")
    void empresa_inexistente_devuelve_vacio() {
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(port.findById(9L)).isEmpty();
    }
}
