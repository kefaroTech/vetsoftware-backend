package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.state.application.port.out.CityChildrenQueryPort;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import com.vetsoftware.app.state.domain.StateHasActiveChildrenException;
import com.vetsoftware.app.state.domain.StateNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteStateService")
class DeleteStateServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Mock
    private StateRepository repository;
    @Mock
    private CityChildrenQueryPort cityChildrenQueryPort;

    @InjectMocks
    private DeleteStateService service;

    private static State antioquia() {
        return new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true);
    }

    @Test
    @DisplayName("borra el departamento cuando existe y no tiene municipios activos")
    void borra_el_departamento_sin_hijos_activos() {
        when(repository.findById(7L)).thenReturn(Optional.of(antioquia()));
        when(cityChildrenQueryPort.existsActiveByStateId(7L)).thenReturn(false);

        service.execute(7L);

        verify(repository).delete(7L);
    }

    @Test
    @DisplayName("un departamento inexistente no se borra ni se consulta por hijos")
    void departamento_inexistente_no_borra_nada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L)).isInstanceOf(StateNotFoundException.class)
                .hasMessageContaining("State not found: 99");

        verify(repository, never()).delete(any());
        verifyNoInteractions(cityChildrenQueryPort);
    }

    @Test
    @DisplayName("con municipios activos rechaza el borrado y no escribe")
    void con_hijos_activos_no_borra() {
        when(repository.findById(7L)).thenReturn(Optional.of(antioquia()));
        when(cityChildrenQueryPort.existsActiveByStateId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(7L))
                .isInstanceOf(StateHasActiveChildrenException.class).hasMessageContaining("7")
                .hasMessageContaining("city");

        verify(repository, never()).delete(any());
    }
}
