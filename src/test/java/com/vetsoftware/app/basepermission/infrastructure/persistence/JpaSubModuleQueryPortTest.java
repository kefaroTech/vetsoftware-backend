package com.vetsoftware.app.basepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code SubModuleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSubModuleQueryPort (basepermission) — resolucion del submodulo referenciado")
class JpaSubModuleQueryPortTest {

    @Mock
    private SubModuleJpaRepository subModuleJpaRepository;
    @Mock
    private SubModuleJpaEntity subModuleEntity;
    @InjectMocks
    private JpaSubModuleQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a SubModuleRef")
        void mapea_la_entidad_encontrada_a_sub_module_ref() {
            when(subModuleEntity.getId()).thenReturn(1L);
            when(subModuleEntity.getName()).thenReturn("Ventas");
            when(subModuleEntity.getCode()).thenReturn("VEN");
            when(subModuleJpaRepository.findById(1L)).thenReturn(Optional.of(subModuleEntity));

            Optional<SubModuleRef> ref = port.findById(1L);

            assertThat(ref).contains(new SubModuleRef(1L, "Ventas", "VEN"));
        }

        @Test
        @DisplayName("devuelve vacio si el submodulo no existe")
        void devuelve_vacio_si_el_submodulo_no_existe() {
            when(subModuleJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
