package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code BaseRoleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica
 * propia: es un portador de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAdminBaseRoleQueryPort — resolucion del rol base ADMIN")
class JpaAdminBaseRoleQueryPortTest {

    @Mock
    private BaseRoleJpaRepository baseRoleJpaRepository;
    @Mock
    private BaseRoleJpaEntity rolBaseAdmin;
    @InjectMocks
    private JpaAdminBaseRoleQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el id del rol base ADMIN cuando esta configurado")
        void devuelve_el_id_del_rol_base_admin() {
            when(rolBaseAdmin.getId()).thenReturn(3L);
            when(baseRoleJpaRepository.findByCode("ADMIN")).thenReturn(Optional.of(rolBaseAdmin));

            assertThat(port.findAdminBaseRoleId()).contains(3L);
        }

        @Test
        @DisplayName("sin rol base ADMIN configurado devuelve vacio")
        void sin_rol_base_admin_devuelve_vacio() {
            when(baseRoleJpaRepository.findByCode("ADMIN")).thenReturn(Optional.empty());

            assertThat(port.findAdminBaseRoleId()).isEmpty();
        }
    }
}
