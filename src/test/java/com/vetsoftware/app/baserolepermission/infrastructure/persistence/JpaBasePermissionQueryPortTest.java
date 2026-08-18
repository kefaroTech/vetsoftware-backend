package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaEntity;
import com.vetsoftware.app.basepermission.infrastructure.persistence.BasePermissionJpaRepository;
import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code BasePermissionJpaEntity} se mockea porque su constructor sin
 * argumentos es {@code protected} y no es instanciable desde este paquete. No
 * tiene logica: es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBasePermissionQueryPort — resolucion del permiso base referenciado")
class JpaBasePermissionQueryPortTest {

    @Mock
    private BasePermissionJpaRepository basePermissionJpaRepository;
    @Mock
    private BasePermissionJpaEntity basePermissionEntity;
    @InjectMocks
    private JpaBasePermissionQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a BasePermissionRef")
        void mapea_la_entidad_encontrada_a_base_permission_ref() {
            when(basePermissionEntity.getId()).thenReturn(10L);
            when(basePermissionEntity.getName()).thenReturn("Crear consulta");
            when(basePermissionEntity.getCode()).thenReturn("CONSULTA_CREATE");
            when(basePermissionJpaRepository.findById(10L))
                    .thenReturn(Optional.of(basePermissionEntity));

            Optional<BasePermissionRef> ref = port.findById(10L);

            assertThat(ref)
                    .contains(new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"));
        }

        @Test
        @DisplayName("devuelve vacio si el permiso base no existe")
        void devuelve_vacio_si_el_permiso_base_no_existe() {
            when(basePermissionJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
