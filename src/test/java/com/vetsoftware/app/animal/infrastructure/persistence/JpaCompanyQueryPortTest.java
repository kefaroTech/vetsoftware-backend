package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
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
            when(companyEntity.getId()).thenReturn(AnimalMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(AnimalMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(AnimalMother.CLINICA.identifier());
            when(companyJpaRepository.findById(AnimalMother.CLINICA.id()))
                    .thenReturn(Optional.of(companyEntity));

            Optional<CompanyRef> resultado = port.findById(AnimalMother.CLINICA.id());

            assertThat(resultado).contains(AnimalMother.CLINICA);
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacio")
        void una_empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(AnimalMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            Optional<CompanyRef> resultado = port.findById(AnimalMother.CLINICA.id());

            assertThat(resultado).isEmpty();
        }
    }
}
