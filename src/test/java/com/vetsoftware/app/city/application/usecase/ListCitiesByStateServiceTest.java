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
@DisplayName("ListCitiesByStateService")
class ListCitiesByStateServiceTest {

    @Mock
    private CityRepository repository;

    private ListCitiesByStateService service;

    @BeforeEach
    void crearServicio() {
        service = new ListCitiesByStateService(repository);
    }

    @Nested
    @DisplayName("listado por departamento")
    class ListadoPorDepartamento {

        @Test
        @DisplayName("mapea cada ciudad del departamento a su dto")
        void mapea_cada_ciudad_del_departamento_a_su_dto() {
            when(repository.findByStateId(CityMother.STATE_ID))
                    .thenReturn(List.of(CityMother.activa()));

            List<CityDto> resultado = service.listByState(CityMother.STATE_ID);

            assertThat(resultado).extracting(CityDto::id).containsExactly(CityMother.CITY_ID);
        }

        @Test
        @DisplayName("un departamento sin ciudades devuelve una lista vacia")
        void un_departamento_sin_ciudades_devuelve_una_lista_vacia() {
            when(repository.findByStateId(CityMother.STATE_ID)).thenReturn(List.of());

            List<CityDto> resultado = service.listByState(CityMother.STATE_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
