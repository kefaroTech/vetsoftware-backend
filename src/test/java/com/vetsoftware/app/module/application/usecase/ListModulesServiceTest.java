package com.vetsoftware.app.module.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.testsupport.ModuleMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListModulesService")
class ListModulesServiceTest {

    @Mock
    private ModuleRepository repository;

    private ListModulesService service;

    @BeforeEach
    void crearServicio() {
        service = new ListModulesService(repository);
    }

    @Test
    @DisplayName("lista todos los modulos mapeados a dto")
    void lista_todos_los_modulos() {
        when(repository.findAll()).thenReturn(List.of(ModuleMother.moduloValido()));

        List<ModuleDto> resultado = service.listAll();

        assertThat(resultado).extracting(ModuleDto::id).containsExactly(1L);
    }

    @Test
    @DisplayName("sin modulos recibe una lista vacia")
    void sin_modulos_recibe_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
