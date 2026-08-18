package com.vetsoftware.app.baserole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaRepository;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaEntity;
import com.vetsoftware.app.baserolepermission.infrastructure.persistence.BaseRolePermissionJpaRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BasePermissionJpaEntity/BaseRoleJpaEntity de otra feature tienen constructor
 * sin argumentos protected: se mockean como dobles de datos, igual que hace
 * AnimalJpaMapperTest con las entidades cruzadas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBaseRolePermissionInitializationPort")
class JpaBaseRolePermissionInitializationPortTest {

    @Mock
    private BaseRoleJpaRepository baseRoleJpaRepository;
    @Mock
    private BasePermissionJpaRepository basePermissionJpaRepository;
    @Mock
    private BaseRolePermissionJpaRepository baseRolePermissionJpaRepository;
    @Mock
    private BasePermissionJpaEntity permisoUno;
    @Mock
    private BasePermissionJpaEntity permisoDos;

    @InjectMocks
    private JpaBaseRolePermissionInitializationPort port;

    @Nested
    @DisplayName("permisos ya vinculados")
    class PermisosYaVinculados {

        @Test
        @DisplayName("solo crea vinculos para los permisos que faltan")
        void solo_crea_vinculos_para_los_permisos_que_faltan() {
            BaseRoleJpaEntity baseRole = new BaseRoleJpaEntity();
            baseRole.setId(10L);
            when(baseRoleJpaRepository.getReferenceById(10L)).thenReturn(baseRole);
            when(permisoUno.getId()).thenReturn(1L);
            when(permisoDos.getId()).thenReturn(2L);
            when(basePermissionJpaRepository.findAll()).thenReturn(List.of(permisoUno, permisoDos));
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(10L, 1L))
                    .thenReturn(true);
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(10L, 2L))
                    .thenReturn(false);

            port.initializeForAllBasePermissions(10L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<BaseRolePermissionJpaEntity>> captor = ArgumentCaptor
                    .forClass(List.class);
            verify(baseRolePermissionJpaRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getBasePermission()).isSameAs(permisoDos);
            assertThat(captor.getValue().get(0).getBaseRole()).isSameAs(baseRole);
        }
    }

    @Nested
    @DisplayName("sin ningun vinculo previo")
    class SinVinculoPrevio {

        @Test
        @DisplayName("crea un vinculo por cada permiso base existente")
        void crea_un_vinculo_por_cada_permiso_base_existente() {
            BaseRoleJpaEntity baseRole = new BaseRoleJpaEntity();
            baseRole.setId(11L);
            when(baseRoleJpaRepository.getReferenceById(11L)).thenReturn(baseRole);
            when(permisoUno.getId()).thenReturn(1L);
            when(permisoDos.getId()).thenReturn(2L);
            when(basePermissionJpaRepository.findAll()).thenReturn(List.of(permisoUno, permisoDos));
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(11L, 1L))
                    .thenReturn(false);
            when(baseRolePermissionJpaRepository.existsByBaseRoleIdAndBasePermissionId(11L, 2L))
                    .thenReturn(false);

            port.initializeForAllBasePermissions(11L);

            verify(baseRolePermissionJpaRepository).saveAll(anyList());
        }
    }
}
