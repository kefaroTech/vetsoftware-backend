package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListStatesService")
class ListStatesServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Mock
    private StateRepository repository;

    @InjectMocks
    private ListStatesService service;

    @Test
    @DisplayName("proyecta cada departamento a su dto respetando el orden del repositorio")
    void proyecta_cada_departamento_respetando_el_orden() {
        when(repository.findAll())
                .thenReturn(List.of(new State(1L, "Antioquia", COLOMBIA, "05", CREACION, true),
                        new State(2L, "Cundinamarca", COLOMBIA, "25", CREACION, false)));

        List<StateDto> dtos = service.listAll();

        assertThat(dtos).extracting(StateDto::id, StateDto::name, StateDto::enabled)
                .containsExactly(tuple(1L, "Antioquia", true), tuple(2L, "Cundinamarca", false));
    }

    @Test
    @DisplayName("sin departamentos devuelve una lista vacia, nunca null")
    void sin_departamentos_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
