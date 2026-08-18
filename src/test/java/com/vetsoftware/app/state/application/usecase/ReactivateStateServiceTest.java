package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
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
@DisplayName("ReactivateStateService")
class ReactivateStateServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Mock
    private StateRepository repository;

    @InjectMocks
    private ReactivateStateService service;

    @Test
    @DisplayName("reactiva el departamento y devuelve su estado ya habilitado")
    void reactiva_y_devuelve_el_estado_habilitado() {
        when(repository.reactivate(7L)).thenReturn(1);
        when(repository.findById(7L)).thenReturn(
                Optional.of(new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true)));

        StateDto dto = service.execute(7L);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Antioquia");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("si la reactivacion no afecta filas lanza 404 y no vuelve a leer")
    void sin_filas_afectadas_lanza_not_found() {
        when(repository.reactivate(99L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(99L)).isInstanceOf(StateNotFoundException.class)
                .hasMessageContaining("State not found: 99");

        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("si la fila desaparece entre el update y la relectura lanza 404")
    void relectura_vacia_lanza_not_found() {
        when(repository.reactivate(7L)).thenReturn(1);
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(7L)).isInstanceOf(StateNotFoundException.class)
                .hasMessageContaining("State not found: 7");
    }
}
