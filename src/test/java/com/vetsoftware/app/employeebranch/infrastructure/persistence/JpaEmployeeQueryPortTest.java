package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeQueryPort (employeebranch)")
class JpaEmployeeQueryPortTest {

    private static final Long EMPLOYEE_ID = 940L;
    private static final Long COMPANY_ID = 900L;

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;

    @InjectMocks
    private JpaEmployeeQueryPort port;

    @Test
    @DisplayName("existe si hay una fila activa de ese empleado en esa empresa")
    void existe_si_hay_una_fila_activa_en_la_empresa() {
        when(employeeJpaRepository.findByIdAndCompany_Id(EMPLOYEE_ID, COMPANY_ID))
                .thenReturn(Optional.of(mock(EmployeeJpaEntity.class)));

        assertThat(port.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).isTrue();
    }

    @Test
    @DisplayName("no existe si no hay fila activa para esa empresa (inexistente o de otro tenant)")
    void no_existe_si_no_hay_fila_activa() {
        when(employeeJpaRepository.findByIdAndCompany_Id(EMPLOYEE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThat(port.existsByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).isFalse();
    }
}
