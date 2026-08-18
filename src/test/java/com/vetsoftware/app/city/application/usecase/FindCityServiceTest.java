package com.vetsoftware.app.city.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import com.vetsoftware.app.city.testsupport.CityMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCityService")
class FindCityServiceTest {

    @Mock
    private CityRepository repository;

    private FindCityService service;

    @BeforeEach
    void crearServicio() {
        service = new FindCityService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve la ciudad encontrada por el repositorio")
        void devuelve_la_ciudad_encontrada() {
            City ciudad = CityMother.activa();
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.of(ciudad));

            CityDto dto = service.findById(CityMother.CITY_ID);

            assertThat(dto.id()).isEqualTo(CityMother.CITY_ID);
            assertThat(dto.name()).isEqualTo("Medellin");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("lanza no encontrada si no existe")
        void lanza_no_encontrada_si_no_existe() {
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(CityMother.CITY_ID))
                    .isInstanceOf(CityNotFoundException.class)
                    .hasMessageContaining("City not found: " + CityMother.CITY_ID);
        }
    }
}
