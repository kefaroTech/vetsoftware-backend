package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @InjectMocks
    private JpaCompanyQueryPort port;

    private static CompanyJpaEntity companyEncontrada(long id, String name, String identifier) {
        CompanyJpaEntity entity = mock(CompanyJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        when(entity.getIdentifier()).thenReturn(identifier);
        return entity;
    }

    @Test
    @DisplayName("mapea la empresa encontrada a su companion VO")
    void mapea_la_empresa_encontrada_a_su_companion_vo() {
        CompanyJpaEntity company = companyEncontrada(9L, "Clinica Norte", "900123456");
        when(companyJpaRepository.findById(9L)).thenReturn(Optional.of(company));

        Optional<CompanyRef> ref = port.findById(9L);

        assertThat(ref).contains(new CompanyRef(9L, "Clinica Norte", "900123456"));
    }

    @Test
    @DisplayName("devuelve vacio si la empresa no existe")
    void devuelve_vacio_si_la_empresa_no_existe() {
        when(companyJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
