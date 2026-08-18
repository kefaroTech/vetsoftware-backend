package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
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
@DisplayName("JpaEmployeeQueryPort (debtopenaccount) — resuelve el EmployeeRef acotado a la empresa")
class JpaEmployeeQueryPortTest {

    private static final Long EMPLOYEE_ID = 7L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;

    @InjectMocks
    private JpaEmployeeQueryPort port;

    @Test
    @DisplayName("mapea el empleado encontrado en la empresa al EmployeeRef")
    void mapea_el_empleado_encontrado_en_la_empresa() {
        EmployeeJpaEntity entity = mock(EmployeeJpaEntity.class);
        when(entity.getId()).thenReturn(EMPLOYEE_ID);
        when(entity.getName()).thenReturn("Ana Ruiz");
        when(employeeJpaRepository.findByIdAndCompany_Id(EMPLOYEE_ID, COMPANY_ID))
                .thenReturn(Optional.of(entity));

        assertThat(port.findByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID))
                .contains(new EmployeeRef(EMPLOYEE_ID, "Ana Ruiz"));
    }

    @Test
    @DisplayName("vacio si el empleado no existe o es de otra empresa")
    void vacio_si_el_empleado_no_existe_o_es_de_otra_empresa() {
        when(employeeJpaRepository.findByIdAndCompany_Id(EMPLOYEE_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(EMPLOYEE_ID, COMPANY_ID)).isEmpty();
    }
}
