package com.vetsoftware.app.coderecovery.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import com.vetsoftware.app.coderecovery.testsupport.CodeRecoveryMother;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeAccountsByEmailPort")
class JpaEmployeeAccountsByEmailPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaEmployeeAccountsByEmailPort port;

    @Test
    @DisplayName("mapea las cuentas activas y verificadas a EmployeeAccount")
    void mapea_las_cuentas_activas_y_verificadas() throws ReflectiveOperationException {
        CompanyJpaEntity company = CodeRecoveryMother.companyJpa("Veterinaria Central");
        EmployeeJpaEntity empleado = CodeRecoveryMother.empleadoJpa("Juan Pérez", "EMP001",
                company);
        when(employeeJpaRepository.findByEmailAndEmailVerified("a@b.com", true))
                .thenReturn(List.of(empleado));

        List<EmployeeAccount> result = port.findByEmail("a@b.com");

        assertThat(result).containsExactly(
                new EmployeeAccount("Juan Pérez", "EMP001", "Veterinaria Central"));
    }

    @Test
    @DisplayName("sin coincidencias devuelve lista vacía")
    void sin_coincidencias_devuelve_lista_vacia() {
        when(employeeJpaRepository.findByEmailAndEmailVerified("desconocido@b.com", true))
                .thenReturn(List.of());

        List<EmployeeAccount> result = port.findByEmail("desconocido@b.com");

        assertThat(result).isEmpty();
    }
}
