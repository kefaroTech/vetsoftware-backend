package com.vetsoftware.app.role.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.role.testsupport.RoleMother;
import com.vetsoftware.app.rolepermission.infrastructure.persistence.RolePermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaRolePermissionChildrenCascadePort — adaptador sobre RolePermissionJpaRepository")
class JpaRolePermissionChildrenCascadePortTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long OTRA_EMPRESA = 99L;

    @Mock
    private RolePermissionJpaRepository jpaRepository;

    @InjectMocks
    private JpaRolePermissionChildrenCascadePort port;

    @Test
    @DisplayName("delega la desactivacion en cascada y devuelve las filas afectadas")
    void delega_la_desactivacion_en_cascada() {
        when(jpaRepository.disableAllByRoleId(RoleMother.ROLE_ID, COMPANY_ID)).thenReturn(3);

        int filas = port.deactivateAllByRoleId(RoleMother.ROLE_ID, COMPANY_ID);

        assertThat(filas).isEqualTo(3);
    }

    @Test
    @DisplayName("un rol sin permisos activos no afecta filas")
    void un_rol_sin_permisos_activos_no_afecta_filas() {
        when(jpaRepository.disableAllByRoleId(RoleMother.ROLE_ID, COMPANY_ID)).thenReturn(0);

        assertThat(port.deactivateAllByRoleId(RoleMother.ROLE_ID, COMPANY_ID)).isZero();
    }

    @Test
    @DisplayName("la empresa viaja al UPDATE: el rol de otro tenant no afecta ninguna fila")
    void la_empresa_viaja_al_update() {
        when(jpaRepository.disableAllByRoleId(RoleMother.ROLE_ID, OTRA_EMPRESA)).thenReturn(0);

        assertThat(port.deactivateAllByRoleId(RoleMother.ROLE_ID, OTRA_EMPRESA)).isZero();

        verify(jpaRepository).disableAllByRoleId(RoleMother.ROLE_ID, OTRA_EMPRESA);
        verify(jpaRepository, never()).disableAllByIds(anyCollection());
    }
}
