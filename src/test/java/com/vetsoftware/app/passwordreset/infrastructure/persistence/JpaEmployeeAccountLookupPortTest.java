package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeeAccountLookupPort.EmployeeAccount;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeAccountLookupPort")
class JpaEmployeeAccountLookupPortTest {

    private static final String EMPLOYEE_CODE = "EMP001";

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @Mock
    private EmployeeJpaEntity employeeEntity;
    @Mock
    private CompanyJpaEntity companyEntity;

    @InjectMocks
    private JpaEmployeeAccountLookupPort port;

    @Test
    @DisplayName("mapea la cuenta con la company resuelta por el @EntityGraph de findByEmployeeCode")
    void mapea_la_cuenta_con_la_company_resuelta() {
        when(employeeJpaRepository.findByEmployeeCode(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employeeEntity));
        when(employeeEntity.getId()).thenReturn(500L);
        when(employeeEntity.getName()).thenReturn("Ana Ruiz");
        when(employeeEntity.getEmail()).thenReturn("ana@vetrina.co");
        when(employeeEntity.isEmailVerified()).thenReturn(true);
        when(employeeEntity.getCompany()).thenReturn(companyEntity);
        when(companyEntity.getId()).thenReturn(9L);
        when(companyEntity.getName()).thenReturn("Clinica Norte");

        Optional<EmployeeAccount> account = port.findByCode(EMPLOYEE_CODE);

        assertThat(account).isPresent();
        EmployeeAccount cuenta = account.orElseThrow();
        assertThat(cuenta.id()).isEqualTo(500L);
        assertThat(cuenta.companyId()).isEqualTo(9L);
        assertThat(cuenta.name()).isEqualTo("Ana Ruiz");
        assertThat(cuenta.email()).isEqualTo("ana@vetrina.co");
        assertThat(cuenta.companyName()).isEqualTo("Clinica Norte");
        assertThat(cuenta.emailVerified()).isTrue();
    }

    @Test
    @DisplayName("codigo inexistente (o empleado deshabilitado, filtrado por @SQLRestriction) devuelve vacio")
    void codigo_inexistente_devuelve_vacio() {
        when(employeeJpaRepository.findByEmployeeCode(EMPLOYEE_CODE)).thenReturn(Optional.empty());

        assertThat(port.findByCode(EMPLOYEE_CODE)).isEmpty();
    }

    @Test
    @DisplayName("propaga correo no verificado: la decision de elegibilidad no se toma aqui")
    void propaga_correo_no_verificado() {
        when(employeeJpaRepository.findByEmployeeCode(EMPLOYEE_CODE))
                .thenReturn(Optional.of(employeeEntity));
        when(employeeEntity.getId()).thenReturn(500L);
        when(employeeEntity.getName()).thenReturn("Ana Ruiz");
        when(employeeEntity.getEmail()).thenReturn("ana@vetrina.co");
        when(employeeEntity.isEmailVerified()).thenReturn(false);
        when(employeeEntity.getCompany()).thenReturn(companyEntity);
        when(companyEntity.getId()).thenReturn(9L);
        when(companyEntity.getName()).thenReturn("Clinica Norte");

        Optional<EmployeeAccount> account = port.findByCode(EMPLOYEE_CODE);

        assertThat(account).isPresent();
        assertThat(account.orElseThrow().emailVerified()).isFalse();
    }
}
