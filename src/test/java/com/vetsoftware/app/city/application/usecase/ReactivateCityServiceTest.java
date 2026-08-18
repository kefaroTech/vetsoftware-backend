package com.vetsoftware.app.city.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateCityService")
class ReactivateCityServiceTest {

    @Mock
    private CityRepository repository;

    private ReactivateCityService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateCityService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la ciudad releida")
        void reactiva_y_devuelve_la_ciudad_releida() {
            City reactivada = CityMother.activa();
            when(repository.reactivate(CityMother.CITY_ID)).thenReturn(1);
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.of(reactivada));

            CityDto dto = service.execute(CityMother.CITY_ID);

            assertThat(dto.id()).isEqualTo(CityMother.CITY_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("ninguna fila afectada no vuelve a leer y lanza no encontrada")
        void ninguna_fila_afectada_no_vuelve_a_leer() {
            when(repository.reactivate(CityMother.CITY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(CityMother.CITY_ID))
                    .isInstanceOf(CityNotFoundException.class)
                    .hasMessageContaining("City not found: " + CityMother.CITY_ID);

            verify(repository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("una fila reactivada pero ilocalizable tambien lanza no encontrada")
        void una_fila_reactivada_pero_ilocalizable_tambien_lanza_no_encontrada() {
            when(repository.reactivate(CityMother.CITY_ID)).thenReturn(1);
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CityMother.CITY_ID))
                    .isInstanceOf(CityNotFoundException.class)
                    .hasMessageContaining("City not found: " + CityMother.CITY_ID);
        }
    }
}
