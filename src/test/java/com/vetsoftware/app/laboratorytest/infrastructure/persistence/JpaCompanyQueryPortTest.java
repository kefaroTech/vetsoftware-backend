package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort — adaptador sobre CompanyJpaRepository")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;
    @InjectMocks
    private JpaCompanyQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la empresa encontrada a su companion VO")
        void mapea_la_empresa_encontrada_a_su_companion_vo() {
            CompanyRef clinica = LaboratoryTestMother.CLINICA;
            when(companyEntity.getId()).thenReturn(clinica.id());
            when(companyEntity.getName()).thenReturn(clinica.name());
            when(companyEntity.getIdentifier()).thenReturn(clinica.identifier());
            when(companyJpaRepository.findById(clinica.id()))
                    .thenReturn(Optional.of(companyEntity));

            Optional<CompanyRef> resultado = port.findById(clinica.id());

            assertThat(resultado).contains(clinica);
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacio")
        void una_empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThat(port.findById(999L)).isEmpty();
        }
    }
}
