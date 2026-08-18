package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.EMPLEADO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeQueryPort (servicechargeopenaccount)")
class JpaEmployeeQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @Mock
    private EmployeeJpaEntity employeeEntity;

    @InjectMocks
    private JpaEmployeeQueryPort port;

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea el empleado encontrado a su companion VO")
        void mapea_el_empleado_encontrado_a_su_companion_vo() {
            when(employeeJpaRepository.findByIdAndCompany_Id(EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.of(employeeEntity));
            when(employeeEntity.getId()).thenReturn(EMPLEADO.id());
            when(employeeEntity.getName()).thenReturn(EMPLEADO.name());

            Optional<EmployeeRef> found = port.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID);

            assertThat(found).contains(EMPLEADO);
        }

        @Test
        @DisplayName("un empleado de otra empresa no aparece")
        void un_empleado_de_otra_empresa_no_aparece() {
            when(employeeJpaRepository.findByIdAndCompany_Id(EMPLEADO.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(EMPLEADO.id(), COMPANY_ID)).isEmpty();
        }
    }
}
