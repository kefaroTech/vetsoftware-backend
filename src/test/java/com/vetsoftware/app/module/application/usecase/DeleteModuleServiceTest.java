package com.vetsoftware.app.module.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.application.port.out.SubModuleChildrenQueryPort;
import com.vetsoftware.app.module.domain.ModuleHasActiveChildrenException;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import com.vetsoftware.app.module.testsupport.ModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteModuleService")
class DeleteModuleServiceTest {

    @Mock
    private ModuleRepository repository;
    @Mock
    private SubModuleChildrenQueryPort subModuleChildrenQueryPort;

    private DeleteModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteModuleService(repository, subModuleChildrenQueryPort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra el modulo sin sub-modulos activos")
        void borra_el_modulo_sin_hijos_activos() {
            when(repository.findById(1L)).thenReturn(Optional.of(ModuleMother.moduloValido()));
            when(subModuleChildrenQueryPort.existsActiveByModuleId(1L)).thenReturn(false);

            service.execute(1L);

            verify(repository).delete(1L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no borra si el modulo no existe")
        void no_borra_si_el_modulo_no_existe() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(ModuleNotFoundException.class).hasMessageContaining("1");

            verifyNoInteractions(subModuleChildrenQueryPort);
            verify(repository, never()).delete(1L);
        }

        @Test
        @DisplayName("no borra un modulo con sub-modulos activos")
        void no_borra_un_modulo_con_hijos_activos() {
            when(repository.findById(1L)).thenReturn(Optional.of(ModuleMother.moduloValido()));
            when(subModuleChildrenQueryPort.existsActiveByModuleId(1L)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(1L))
                    .isInstanceOf(ModuleHasActiveChildrenException.class).hasMessageContaining("1")
                    .hasMessageContaining("subModule");

            verify(repository, never()).delete(1L);
        }
    }
}
