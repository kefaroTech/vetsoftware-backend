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
@DisplayName("ListBreedsService")
class ListBreedsServiceTest {

    @Mock
    private BreedRepository repository;
    @InjectMocks
    private ListBreedsService service;

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada raza del repositorio a dto")
        void mapea_cada_raza_del_repositorio_a_dto() {
            when(repository.findAll()).thenReturn(List.of(BreedMother.labrador()));

            List<BreedDto> resultado = service.listAll();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).name()).isEqualTo("Labrador");
        }

        @Test
        @DisplayName("un catalogo vacio devuelve una lista vacia, no null")
        void un_catalogo_vacio_devuelve_una_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            assertThat(service.listAll()).isEmpty();
        }
    }
}
