package com.vetsoftware.app.module.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateModuleService")
class CreateModuleServiceTest {

    @Mock
    private ModuleRepository repository;

    private CreateModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateModuleService(repository);
    }

    @Test
    @DisplayName("persiste el modulo creado con name y code del comando")
    void persiste_el_modulo_creado() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModuleDto dto = service.execute(new CreateModuleCommand("Inventario", "INV"));

        ArgumentCaptor<Module> guardado = ArgumentCaptor.forClass(Module.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getName()).isEqualTo("Inventario");
        assertThat(guardado.getValue().getCode()).isEqualTo("INV");
        assertThat(dto.name()).isEqualTo("Inventario");
    }
}
