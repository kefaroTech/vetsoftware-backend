package com.vetsoftware.app.submodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindSubModuleService")
class FindSubModuleServiceTest {

    @Mock
    private SubModuleRepository repository;
    @InjectMocks
    private FindSubModuleService service;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el dto del submodulo encontrado")
        void devuelve_el_dto_del_submodulo_encontrado() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(SubModuleMother.reportes()));

            SubModuleDto dto = service.findById(SubModuleMother.SUB_MODULE_ID);

            assertThat(dto.id()).isEqualTo(SubModuleMother.SUB_MODULE_ID);
            assertThat(dto.name()).isEqualTo("Reportes");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza SubModuleNotFoundException si no existe")
        void lanza_submodule_not_found_si_no_existe() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(SubModuleMother.SUB_MODULE_ID))
                    .isInstanceOf(SubModuleNotFoundException.class)
                    .hasMessageContaining("SubModule not found: " + SubModuleMother.SUB_MODULE_ID);
        }
    }
}
