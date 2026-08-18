package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.out.CountryQueryPort;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateStateService")
class UpdateStateServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");
    private static final CountryRef CHILE = new CountryRef(2L, "Chile");

    @Mock
    private StateRepository repository;
    @Mock
    private CountryQueryPort countryQueryPort;

    @InjectMocks
    private UpdateStateService service;

    @Captor
    private ArgumentCaptor<State> stateCaptor;

    private static State antioquia() {
        return new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true);
    }

    @Test
    @DisplayName("guarda el departamento con el nombre, pais y dane code nuevos")
    void guarda_los_campos_nuevos() {
        when(repository.findById(7L)).thenReturn(Optional.of(antioquia()));
        when(countryQueryPort.findById(2L)).thenReturn(Optional.of(CHILE));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new UpdateStateCommand(7L, "Antioquia Renombrada", 2L, "06"));

        verify(repository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getName()).isEqualTo("Antioquia Renombrada");
        assertThat(stateCaptor.getValue().getCountry()).isEqualTo(CHILE);
        assertThat(stateCaptor.getValue().getDaneCode()).isEqualTo("06");
        assertThat(stateCaptor.getValue().getId()).isEqualTo(7L);
        assertThat(stateCaptor.getValue().getCreatedDate()).isEqualTo(CREACION);
    }

    @Test
    @DisplayName("devuelve el dto del departamento actualizado")
    void devuelve_el_dto_actualizado() {
        when(repository.findById(7L)).thenReturn(Optional.of(antioquia()));
        when(countryQueryPort.findById(1L)).thenReturn(Optional.of(COLOMBIA));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StateDto dto = service.execute(new UpdateStateCommand(7L, "Antioquia", 1L, "05"));

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Antioquia");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("si el departamento no existe lanza 404 de dominio y no consulta el pais")
    void departamento_inexistente_no_guarda_nada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new UpdateStateCommand(99L, "Chile", 1L, "05")))
                .isInstanceOf(StateNotFoundException.class)
                .hasMessageContaining("State not found: 99");

        verify(repository, never()).save(any());
        verifyNoInteractions(countryQueryPort);
    }

    @Test
    @DisplayName("un pais inexistente no guarda nada")
    void pais_inexistente_no_guarda_nada() {
        when(repository.findById(7L)).thenReturn(Optional.of(antioquia()));
        when(countryQueryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(new UpdateStateCommand(7L, "Antioquia", 99L, "05")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country not found: 99");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un nombre vacio rechaza la actualizacion y no guarda nada")
    void nombre_vacio_no_guarda_nada() {
        when(repository.findById(7L)).thenReturn(Optional.of(antioquia()));
        when(countryQueryPort.findById(1L)).thenReturn(Optional.of(COLOMBIA));

        assertThatThrownBy(() -> service.execute(new UpdateStateCommand(7L, "  ", 1L, "05")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        verify(repository, never()).save(any());
    }
}
