package com.vetsoftware.app.city.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.testsupport.CityMother;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCitiesService")
class ListCitiesServiceTest {

    @Mock
    private CityRepository repository;

    private ListCitiesService service;

    @BeforeEach
    void crearServicio() {
        service = new ListCitiesService(repository);
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("mapea cada ciudad del catalogo global a su dto")
        void mapea_cada_ciudad_a_su_dto() {
            when(repository.findAll()).thenReturn(List.of(CityMother.activa()));

            List<CityDto> resultado = service.listAll();

            assertThat(resultado).extracting(CityDto::id).containsExactly(CityMother.CITY_ID);
        }

        @Test
        @DisplayName("un catalogo vacio devuelve una lista vacia")
        void un_catalogo_vacio_devuelve_una_lista_vacia() {
            when(repository.findAll()).thenReturn(List.of());

            List<CityDto> resultado = service.listAll();

            assertThat(resultado).isEmpty();
        }
    }
}
