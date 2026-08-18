package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.application.port.out.EmployeeProfileQueryPort.EmployeeProfile;
import com.vetsoftware.app.auth.testsupport.ReflectionEntities;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
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
class JpaEmployeeProfileQueryPortTest {

    @Mock
    private EmployeeJpaRepository employeeJpaRepository;
    @InjectMocks
    private JpaEmployeeProfileQueryPort port;

    @Test
    @DisplayName("mapea el perfil visible en /auth/me, incluida la obligación de cambiar clave")
    void mapea_el_perfil_visible_en_me() throws Exception {
        CompanyJpaEntity company = ReflectionEntities.newInstance(CompanyJpaEntity.class);
        company.setId(3L);
        EmployeeJpaEntity entity = ReflectionEntities.newInstance(EmployeeJpaEntity.class);
        entity.setId(7L);
        entity.setCompany(company);
        entity.setName("Ana Ruiz");
        entity.setEmployeeCode("EMP-1");
        entity.setMustChangePassword(true);
        when(employeeJpaRepository.findById(7L)).thenReturn(Optional.of(entity));

        assertThat(port.findById(7L))
                .contains(new EmployeeProfile(7L, 3L, "Ana Ruiz", "EMP-1", true));
    }

    @Test
    @DisplayName("un empleado inexistente no aparece")
    void empleado_inexistente_no_aparece() {
        when(employeeJpaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(port.findById(7L)).isEmpty();
    }
}
