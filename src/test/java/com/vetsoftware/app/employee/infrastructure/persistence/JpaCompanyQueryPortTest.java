package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.testsupport.EmployeeMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (employee) — adaptador sobre CompanyJpaRepository")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    private JpaCompanyQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaCompanyQueryPort(companyJpaRepository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la empresa encontrada a su companion VO")
        void mapea_la_empresa_encontrada_a_su_companion_vo() {
            when(companyEntity.getId()).thenReturn(EmployeeMother.COMPANY_ID);
            when(companyEntity.getName()).thenReturn(EmployeeMother.VETRINA.name());
            when(companyEntity.getIdentifier()).thenReturn(EmployeeMother.VETRINA.identifier());
            when(companyJpaRepository.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.of(companyEntity));

            Optional<CompanyRef> resultado = port.findById(EmployeeMother.COMPANY_ID);

            assertThat(resultado).contains(EmployeeMother.VETRINA);
        }

        @Test
        @DisplayName("una empresa inexistente devuelve vacio")
        void una_empresa_inexistente_devuelve_vacio() {
            when(companyJpaRepository.findById(EmployeeMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            Optional<CompanyRef> resultado = port.findById(EmployeeMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
