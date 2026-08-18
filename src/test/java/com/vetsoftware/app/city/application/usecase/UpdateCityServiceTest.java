package com.vetsoftware.app.city.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.application.port.out.StateQueryPort;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.CityNotFoundException;
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
@DisplayName("UpdateCityService")
class UpdateCityServiceTest {

    @Mock
    private CityRepository repository;
    @Mock
    private StateQueryPort stateQueryPort;

    private UpdateCityService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateCityService(repository, stateQueryPort);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("resuelve el departamento y mueve la ciudad existente")
        void resuelve_el_departamento_y_actualiza_la_ciudad() {
            City existente = CityMother.activa();
            UpdateCityCommand command = CityMother.comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.of(existente));
            when(stateQueryPort.findById(command.stateId()))
                    .thenReturn(Optional.of(CityMother.OTRO_ESTADO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CityDto dto = service.execute(command);

            ArgumentCaptor<City> guardada = ArgumentCaptor.forClass(City.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getName()).isEqualTo("Envigado");
            assertThat(guardada.getValue().getState()).isEqualTo(CityMother.OTRO_ESTADO);
            assertThat(guardada.getValue().getDaneCode()).isEqualTo("05266");
            assertThat(dto.name()).isEqualTo("Envigado");
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca el departamento ni persiste si la ciudad no existe")
        void no_toca_el_departamento_ni_persiste_si_la_ciudad_no_existe() {
            UpdateCityCommand command = CityMother.comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(CityNotFoundException.class)
                    .hasMessageContaining("City not found: " + command.id());

            verifyNoInteractions(stateQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste si el departamento no existe")
        void no_persiste_si_el_departamento_no_existe() {
            City existente = CityMother.activa();
            UpdateCityCommand command = CityMother.comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.of(existente));
            when(stateQueryPort.findById(command.stateId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("State not found: " + command.stateId());

            verify(repository, never()).save(any());
        }
    }
}
