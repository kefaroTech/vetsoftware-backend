package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaEntity;
import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
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
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBaseRoleQueryPort — resolucion del rol base referenciado")
class JpaBaseRoleQueryPortTest {

    @Mock
    private BaseRoleJpaRepository baseRoleJpaRepository;
    @Mock
    private BaseRoleJpaEntity baseRoleEntity;
    @InjectMocks
    private JpaBaseRoleQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a BaseRoleRef")
        void mapea_la_entidad_encontrada_a_base_role_ref() {
            when(baseRoleEntity.getId()).thenReturn(1L);
            when(baseRoleEntity.getName()).thenReturn("Veterinario");
            when(baseRoleEntity.getCode()).thenReturn("VET");
            when(baseRoleJpaRepository.findById(1L)).thenReturn(Optional.of(baseRoleEntity));

            Optional<BaseRoleRef> ref = port.findById(1L);

            assertThat(ref).contains(new BaseRoleRef(1L, "Veterinario", "VET"));
        }

        @Test
        @DisplayName("devuelve vacio si el rol base no existe")
        void devuelve_vacio_si_el_rol_base_no_existe() {
            when(baseRoleJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
