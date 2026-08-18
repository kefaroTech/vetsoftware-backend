package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindStateService")
class FindStateServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Mock
    private StateRepository repository;

    @InjectMocks
    private FindStateService service;

    @Test
    @DisplayName("devuelve el dto del departamento existente campo por campo")
    void devuelve_el_dto_del_departamento_existente() {
        when(repository.findById(7L)).thenReturn(
                Optional.of(new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true)));

        StateDto dto = service.findById(7L);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Antioquia");
        assertThat(dto.country().id()).isEqualTo(1L);
        assertThat(dto.daneCode()).isEqualTo("05");
        assertThat(dto.createdDate()).isEqualTo(CREACION);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un departamento deshabilitado se devuelve marcado como tal")
    void devuelve_el_departamento_deshabilitado() {
        when(repository.findById(7L)).thenReturn(
                Optional.of(new State(7L, "Antioquia", COLOMBIA, "05", CREACION, false)));

        assertThat(service.findById(7L).enabled()).isFalse();
    }

    @Test
    @DisplayName("si no existe lanza StateNotFoundException con el id buscado")
    void departamento_inexistente_lanza_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(StateNotFoundException.class)
                .hasMessageContaining("State not found: 99");
    }
}
