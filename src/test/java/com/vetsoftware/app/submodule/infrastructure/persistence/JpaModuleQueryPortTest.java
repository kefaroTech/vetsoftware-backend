package com.vetsoftware.app.submodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaRepository;
import com.vetsoftware.app.submodule.domain.ModuleRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code ModuleJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaModuleQueryPort (submodule) — resolucion del modulo referenciado")
class JpaModuleQueryPortTest {

    @Mock
    private ModuleJpaRepository moduleJpaRepository;
    @Mock
    private ModuleJpaEntity moduleEntity;
    @InjectMocks
    private JpaModuleQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la entidad encontrada a ModuleRef")
        void mapea_la_entidad_encontrada_a_module_ref() {
            when(moduleEntity.getId()).thenReturn(1L);
            when(moduleEntity.getName()).thenReturn("Facturacion");
            when(moduleEntity.getCode()).thenReturn("FACT");
            when(moduleJpaRepository.findById(1L)).thenReturn(Optional.of(moduleEntity));

            Optional<ModuleRef> ref = port.findById(1L);

            assertThat(ref).contains(new ModuleRef(1L, "Facturacion", "FACT"));
        }

        @Test
        @DisplayName("devuelve vacio si el modulo no existe")
        void devuelve_vacio_si_el_modulo_no_existe() {
            when(moduleJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
