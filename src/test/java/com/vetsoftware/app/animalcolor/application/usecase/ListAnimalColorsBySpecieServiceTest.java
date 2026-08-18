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
@DisplayName("ListAnimalColorsBySpecieService")
class ListAnimalColorsBySpecieServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @InjectMocks
    private ListAnimalColorsBySpecieService service;

    @Nested
    @DisplayName("listado por especie")
    class ListadoPorEspecie {

        @Test
        @DisplayName("mapea a dto solo los colores de la especie pedida")
        void mapea_a_dto_solo_los_colores_de_la_especie_pedida() {
            when(repository.findBySpecieId(AnimalColorMother.PERRO.id()))
                    .thenReturn(List.of(AnimalColorMother.negro()));

            List<AnimalColorDto> resultado = service.listBySpecie(AnimalColorMother.PERRO.id());

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).specie().id()).isEqualTo(AnimalColorMother.PERRO.id());
        }

        @Test
        @DisplayName("una especie sin colores devuelve una lista vacia, no null")
        void una_especie_sin_colores_devuelve_una_lista_vacia() {
            when(repository.findBySpecieId(AnimalColorMother.GATO.id())).thenReturn(List.of());

            assertThat(service.listBySpecie(AnimalColorMother.GATO.id())).isEmpty();
        }
    }
}
