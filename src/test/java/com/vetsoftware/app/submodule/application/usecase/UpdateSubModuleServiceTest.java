package com.vetsoftware.app.submodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.out.ModuleQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModule;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
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
@DisplayName("UpdateSubModuleService")
class UpdateSubModuleServiceTest {

    @Mock
    private SubModuleRepository repository;
    @Mock
    private ModuleQueryPort moduleQueryPort;
    @InjectMocks
    private UpdateSubModuleService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza nombre, codigo y modulo y persiste el agregado")
        void actualiza_nombre_codigo_y_modulo_y_persiste_el_agregado() {
            SubModule existente = SubModuleMother.reportes();
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(existente));
            when(moduleQueryPort.findById(SubModuleMother.INVENTARIO.id()))
                    .thenReturn(Optional.of(SubModuleMother.INVENTARIO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubModuleDto dto = service.execute(SubModuleMother.comandoActualizar());

            ArgumentCaptor<SubModule> captor = ArgumentCaptor.forClass(SubModule.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Facturacion Avanzada");
            assertThat(captor.getValue().getCode()).isEqualTo("FACT-AV");
            assertThat(captor.getValue().getModule()).isEqualTo(SubModuleMother.INVENTARIO);
            assertThat(dto.name()).isEqualTo("Facturacion Avanzada");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza SubModuleNotFoundException si el submodulo no existe y no consulta el modulo")
        void lanza_submodule_not_found_si_el_submodulo_no_existe() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SubModuleMother.comandoActualizar()))
                    .isInstanceOf(SubModuleNotFoundException.class)
                    .hasMessageContaining("SubModule not found: " + SubModuleMother.SUB_MODULE_ID);

            verifyNoInteractions(moduleQueryPort);
        }

        @Test
        @DisplayName("no guarda si el modulo destino no existe")
        void no_guarda_si_el_modulo_destino_no_existe() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(SubModuleMother.reportes()));
            when(moduleQueryPort.findById(SubModuleMother.INVENTARIO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SubModuleMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Module not found: " + SubModuleMother.INVENTARIO.id());

            verify(repository, never()).save(any());
        }
    }
}
