package com.vetsoftware.app.breed.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.testsupport.BreedMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListBreedsBySpecieService")
class ListBreedsBySpecieServiceTest {

    @Mock
    private BreedRepository repository;
    @InjectMocks
    private ListBreedsBySpecieService service;

    @Nested
    @DisplayName("listado por especie")
    class ListadoPorEspecie {

        @Test
        @DisplayName("mapea a dto solo las razas de la especie pedida")
        void mapea_a_dto_solo_las_razas_de_la_especie_pedida() {
            when(repository.findBySpecieId(BreedMother.PERRO.id()))
                    .thenReturn(List.of(BreedMother.labrador()));

            List<BreedDto> resultado = service.listBySpecie(BreedMother.PERRO.id());

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).specie().id()).isEqualTo(BreedMother.PERRO.id());
        }

        @Test
        @DisplayName("una especie sin razas devuelve una lista vacia, no null")
        void una_especie_sin_razas_devuelve_una_lista_vacia() {
            when(repository.findBySpecieId(BreedMother.GATO.id())).thenReturn(List.of());

            assertThat(service.listBySpecie(BreedMother.GATO.id())).isEmpty();
        }
    }
}
