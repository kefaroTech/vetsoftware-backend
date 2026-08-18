package com.vetsoftware.app.branch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.domain.CompanyRef;
import com.vetsoftware.app.branch.testsupport.BranchMother;
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
@DisplayName("JpaCompanyQueryPort (branch)")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    @InjectMocks
    private JpaCompanyQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la empresa encontrada a su companion VO")
        void mapea_la_empresa_encontrada_a_su_companion_vo() {
            when(companyJpaRepository.findById(BranchMother.COMPANY_ID))
                    .thenReturn(Optional.of(companyEntity));
            when(companyEntity.getId()).thenReturn(BranchMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(BranchMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(BranchMother.CLINICA.identifier());

            Optional<CompanyRef> found = port.findById(BranchMother.COMPANY_ID);

            assertThat(found).contains(BranchMother.CLINICA);
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacío")
        void una_empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(BranchMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(BranchMother.COMPANY_ID)).isEmpty();
        }
    }
}
