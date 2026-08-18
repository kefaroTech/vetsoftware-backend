package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeChildrenQueryPort (company) — adaptador sobre EmployeeJpaRepository")
class JpaEmployeeChildrenQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;

    private JpaEmployeeChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaEmployeeChildrenQueryPort(employeeJpaRepository);
    }

    @Nested
    @DisplayName("existencia de empleados activos")
    class ExistenciaDeEmpleadosActivos {

        @Test
        @DisplayName("delega en el repositorio de empleados por el id de la empresa")
        void delega_en_el_repositorio_de_empleados() {
            when(employeeJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin empleados devuelve falso")
        void una_empresa_sin_empleados_devuelve_falso() {
            when(employeeJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
