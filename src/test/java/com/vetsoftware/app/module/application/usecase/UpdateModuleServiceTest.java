package com.vetsoftware.app.module.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.application.command.UpdateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.Module;
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
@DisplayName("UpdateModuleService")
class UpdateModuleServiceTest {

    @Mock
    private ModuleRepository repository;

    private UpdateModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateModuleService(repository);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el modulo existente")
        void actualiza_el_modulo_existente() {
            Module existente = ModuleMother.moduloValido();
            when(repository.findById(1L)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ModuleDto dto = service.execute(new UpdateModuleCommand(1L, "Caja", "CAJA"));

            assertThat(dto.name()).isEqualTo("Caja");
            assertThat(dto.code()).isEqualTo("CAJA");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no guarda si el modulo no existe")
        void no_guarda_si_el_modulo_no_existe() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new UpdateModuleCommand(1L, "Caja", "CAJA")))
                    .isInstanceOf(ModuleNotFoundException.class).hasMessageContaining("1");

            verify(repository, never()).save(any());
        }
    }
}
