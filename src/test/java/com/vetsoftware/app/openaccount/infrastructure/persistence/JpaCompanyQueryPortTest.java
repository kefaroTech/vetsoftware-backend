package com.vetsoftware.app.openaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.openaccount.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @InjectMocks
    private JpaCompanyQueryPort port;

    @Test
    @DisplayName("mapea la empresa encontrada a CompanyRef")
    void mapea_la_empresa_encontrada() {
        CompanyJpaEntity entity = mock(CompanyJpaEntity.class);
        when(entity.getId()).thenReturn(9L);
        when(entity.getName()).thenReturn("Vet SAS");
        when(entity.getIdentifier()).thenReturn("900123456");
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(entity));

        assertThat(port.findById(9L)).contains(new CompanyRef(9L, "Vet SAS", "900123456"));
    }

    @Test
    @DisplayName("devuelve vacio si la empresa no existe")
    void devuelve_vacio_si_no_existe() {
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(port.findById(9L)).isEmpty();
    }
}
