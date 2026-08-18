package com.vetsoftware.app.submodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.testsupport.SubModuleMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSubModulesService")
class ListSubModulesServiceTest {

    @Mock
    private SubModuleRepository repository;
    @InjectMocks
    private ListSubModulesService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada submodulo del repositorio a su dto")
        void mapea_cada_submodulo_a_su_dto() {
            when(repository.findAll()).thenReturn(List.of(SubModuleMother.reportes()));

            List<SubModuleDto> dtos = service.listAll();

            assertThat(dtos).extracting(SubModuleDto::name).containsExactly("Reportes");
        }

        @Test
        @DisplayName("una lista vacia en el repositorio devuelve una lista vacia")
        void lista_vacia_devuelve_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
