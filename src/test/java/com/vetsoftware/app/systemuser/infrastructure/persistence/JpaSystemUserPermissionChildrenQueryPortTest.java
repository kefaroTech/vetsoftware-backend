package com.vetsoftware.app.systemuser.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.infrastructure.persistence.SystemUserPermissionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSystemUserPermissionChildrenQueryPort — adaptador sobre SystemUserPermissionJpaRepository")
class JpaSystemUserPermissionChildrenQueryPortTest {

    @Mock
    private SystemUserPermissionJpaRepository jpaRepository;

    @InjectMocks
    private JpaSystemUserPermissionChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsBySystemUser_Id con el mismo id, no con el de otro puerto")
    void delega_en_exists_by_system_user_id() {
        when(jpaRepository.existsBySystemUser_Id(100L)).thenReturn(true);

        boolean resultado = port.existsActiveBySystemUserId(100L);

        assertThat(resultado).isTrue();
        verify(jpaRepository).existsBySystemUser_Id(100L);
        verifyNoMoreInteractions(jpaRepository);
    }

    @Test
    @DisplayName("sin permisos activos para ese usuario, devuelve false")
    void sin_permisos_activos_devuelve_false() {
        when(jpaRepository.existsBySystemUser_Id(100L)).thenReturn(false);

        boolean resultado = port.existsActiveBySystemUserId(100L);

        assertThat(resultado).isFalse();
    }
}
