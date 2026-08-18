package com.vetsoftware.app.animalcolor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.testsupport.AnimalColorMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAnimalColorsService")
class ListAnimalColorsServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @InjectMocks
    private ListAnimalColorsService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada color del repositorio a dto")
        void mapea_cada_color_del_repositorio_a_dto() {
            when(repository.findAll()).thenReturn(List.of(AnimalColorMother.negro()));

            List<AnimalColorDto> resultado = service.listAll();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).name()).isEqualTo("Negro");
        }

        @Test
        @DisplayName("un catalogo vacio devuelve una lista vacia, no null")
        void un_catalogo_vacio_devuelve_una_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
