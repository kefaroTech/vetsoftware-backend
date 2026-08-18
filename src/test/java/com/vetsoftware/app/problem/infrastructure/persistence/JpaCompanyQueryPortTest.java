package com.vetsoftware.app.problem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.problem.domain.CompanyRef;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (problem)")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    @InjectMocks
    private JpaCompanyQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la empresa encontrada a su CompanyRef")
        void mapea_la_empresa_encontrada() {
            CompanyJpaEntity entidad = mock(CompanyJpaEntity.class);
            when(entidad.getId()).thenReturn(ProblemMother.COMPANY_ID);
            when(entidad.getName()).thenReturn("Clinica Norte");
            when(entidad.getIdentifier()).thenReturn("NIT-900123");
            when(companyJpaRepository.findById(ProblemMother.COMPANY_ID))
                    .thenReturn(Optional.of(entidad));

            Optional<CompanyRef> ref = port.findById(ProblemMother.COMPANY_ID);

            assertThat(ref).contains(
                    new CompanyRef(ProblemMother.COMPANY_ID, "Clinica Norte", "NIT-900123"));
        }

        @Test
        @DisplayName("devuelve vacio si la empresa no existe")
        void devuelve_vacio_si_no_existe() {
            when(companyJpaRepository.findById(ProblemMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(ProblemMother.COMPANY_ID)).isEmpty();
        }
    }
}
