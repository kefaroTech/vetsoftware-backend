package com.vetsoftware.app.city.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.application.port.out.StateQueryPort;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.testsupport.CityMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCityService")
class CreateCityServiceTest {

    @Mock
    private CityRepository repository;
    @Mock
    private StateQueryPort stateQueryPort;

    private CreateCityService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateCityService(repository, stateQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve el departamento por el puerto y persiste la ciudad")
        void resuelve_el_departamento_y_persiste_la_ciudad() {
            when(stateQueryPort.findById(CityMother.STATE_ID))
                    .thenReturn(Optional.of(CityMother.ANTIOQUIA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CityDto dto = service.execute(CityMother.comandoCrear());

            ArgumentCaptor<City> guardada = ArgumentCaptor.forClass(City.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Medellin");
            assertThat(guardada.getValue().getState()).isEqualTo(CityMother.ANTIOQUIA);
            assertThat(guardada.getValue().getDaneCode()).isEqualTo("05001");
            assertThat(guardada.getValue().getId()).isNull();
            assertThat(dto.name()).isEqualTo("Medellin");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca el repositorio si el departamento no existe")
        void no_toca_el_repositorio_si_el_departamento_no_existe() {
            when(stateQueryPort.findById(CityMother.STATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CityMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("State not found: " + CityMother.STATE_ID);

            verifyNoInteractions(repository);
        }
    }
}
