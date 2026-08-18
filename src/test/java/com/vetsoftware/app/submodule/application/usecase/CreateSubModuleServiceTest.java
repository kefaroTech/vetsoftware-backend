package com.vetsoftware.app.submodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.out.ModuleQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModule;
import com.vetsoftware.app.submodule.testsupport.SubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSubModuleService")
class CreateSubModuleServiceTest {

    @Mock
    private SubModuleRepository repository;
    @Mock
    private ModuleQueryPort moduleQueryPort;
    @InjectMocks
    private CreateSubModuleService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el submodulo con el modulo resuelto por el puerto")
        void persiste_el_submodulo_con_el_modulo_resuelto() {
            when(moduleQueryPort.findById(SubModuleMother.FACTURACION.id()))
                    .thenReturn(Optional.of(SubModuleMother.FACTURACION));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubModuleDto dto = service.execute(SubModuleMother.comandoCrear());

            ArgumentCaptor<SubModule> captor = ArgumentCaptor.forClass(SubModule.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Reportes");
            assertThat(captor.getValue().getCode()).isEqualTo("REP");
            assertThat(captor.getValue().getModule()).isEqualTo(SubModuleMother.FACTURACION);
            assertThat(dto.name()).isEqualTo("Reportes");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio si el modulo no existe")
        void no_toca_el_repositorio_si_el_modulo_no_existe() {
            when(moduleQueryPort.findById(SubModuleMother.FACTURACION.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SubModuleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Module not found: " + SubModuleMother.FACTURACION.id());

            verifyNoInteractions(repository);
        }
    }
}
