package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
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
        @DisplayName("mapea la entidad encontrada a su companion VO")
        void mapea_la_entidad_encontrada_a_su_companion_vo() {
            when(companyEntity.getId()).thenReturn(VaccinationTypeMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(VaccinationTypeMother.CLINICA.name());
            when(companyEntity.getIdentifier())
                    .thenReturn(VaccinationTypeMother.CLINICA.identifier());
            when(companyJpaRepository.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.of(companyEntity));

            Optional<CompanyRef> resultado = port.findById(VaccinationTypeMother.COMPANY_ID);

            assertThat(resultado).contains(VaccinationTypeMother.CLINICA);
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacio")
        void una_empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(VaccinationTypeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            Optional<CompanyRef> resultado = port.findById(VaccinationTypeMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
