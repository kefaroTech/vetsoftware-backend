package com.vetsoftware.app.module.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
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
@DisplayName("FindModuleService")
class FindModuleServiceTest {

    @Mock
    private ModuleRepository repository;

    private FindModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new FindModuleService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el modulo encontrado mapeado a dto")
        void devuelve_el_modulo_encontrado() {
            when(repository.findById(1L)).thenReturn(Optional.of(ModuleMother.moduloValido()));

            ModuleDto dto = service.findById(1L);

            assertThat(dto.id()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("un modulo inexistente lanza ModuleNotFoundException")
        void modulo_inexistente_lanza_excepcion() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(ModuleNotFoundException.class).hasMessageContaining("99");
        }
    }
}
