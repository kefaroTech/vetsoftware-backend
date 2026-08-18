package com.vetsoftware.app.role.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.employeerole.infrastructure.persistence.EmployeeRoleJpaRepository;
import com.vetsoftware.app.role.testsupport.RoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEmployeeRoleChildrenQueryPort (role) — adaptador sobre EmployeeRoleJpaRepository")
class JpaEmployeeRoleChildrenQueryPortTest {

    @Mock
    private EmployeeRoleJpaRepository jpaRepository;

    @InjectMocks
    private JpaEmployeeRoleChildrenQueryPort port;

    @Test
    @DisplayName("un rol con asignaciones activas tiene hijos")
    void un_rol_con_asignaciones_activas_tiene_hijos() {
        when(jpaRepository.existsByRole_Id(RoleMother.ROLE_ID)).thenReturn(true);

        assertThat(port.existsActiveByRoleId(RoleMother.ROLE_ID)).isTrue();
    }

    @Test
    @DisplayName("un rol sin asignaciones no tiene hijos")
    void un_rol_sin_asignaciones_no_tiene_hijos() {
        when(jpaRepository.existsByRole_Id(RoleMother.ROLE_ID)).thenReturn(false);

        assertThat(port.existsActiveByRoleId(RoleMother.ROLE_ID)).isFalse();
    }
}
