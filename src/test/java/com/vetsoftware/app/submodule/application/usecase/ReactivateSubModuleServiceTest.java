package com.vetsoftware.app.submodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import com.vetsoftware.app.submodule.testsupport.SubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSubModuleService")
class ReactivateSubModuleServiceTest {

    @Mock
    private SubModuleRepository repository;
    @InjectMocks
    private ReactivateSubModuleService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el submodulo releido")
        void reactiva_y_devuelve_el_submodulo_releido() {
            when(repository.reactivate(SubModuleMother.SUB_MODULE_ID)).thenReturn(1);
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(SubModuleMother.reportes()));

            SubModuleDto dto = service.execute(SubModuleMother.SUB_MODULE_ID);

            assertThat(dto.id()).isEqualTo(SubModuleMother.SUB_MODULE_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza SubModuleNotFoundException si no reactivo ninguna fila y no relee")
        void lanza_submodule_not_found_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(SubModuleMother.SUB_MODULE_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(SubModuleMother.SUB_MODULE_ID))
                    .isInstanceOf(SubModuleNotFoundException.class)
                    .hasMessageContaining("SubModule not found: " + SubModuleMother.SUB_MODULE_ID);

            verify(repository, never()).findById(SubModuleMother.SUB_MODULE_ID);
        }

        @Test
        @DisplayName("lanza SubModuleNotFoundException si reactivo la fila pero desaparecio antes de la relectura")
        void lanza_submodule_not_found_si_desaparece_entre_reactivar_y_releer() {
            when(repository.reactivate(SubModuleMother.SUB_MODULE_ID)).thenReturn(1);
            when(repository.findById(SubModuleMother.SUB_MODULE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SubModuleMother.SUB_MODULE_ID))
                    .isInstanceOf(SubModuleNotFoundException.class)
                    .hasMessageContaining("SubModule not found: " + SubModuleMother.SUB_MODULE_ID);
        }
    }
}
