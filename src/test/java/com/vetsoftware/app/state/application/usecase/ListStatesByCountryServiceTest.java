package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("ListStatesByCountryService")
class ListStatesByCountryServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Mock
    private StateRepository repository;

    @InjectMocks
    private ListStatesByCountryService service;

    @Test
    @DisplayName("proyecta solo los departamentos del pais pedido")
    void proyecta_solo_los_departamentos_del_pais_pedido() {
        when(repository.findByCountryId(1L))
                .thenReturn(List.of(new State(1L, "Antioquia", COLOMBIA, "05", CREACION, true)));

        List<StateDto> dtos = service.listByCountry(1L);

        assertThat(dtos).extracting(StateDto::id).containsExactly(1L);
    }

    @Test
    @DisplayName("un pais sin departamentos devuelve una lista vacia, nunca null")
    void pais_sin_departamentos_devuelve_lista_vacia() {
        when(repository.findByCountryId(99L)).thenReturn(List.of());

        assertThat(service.listByCountry(99L)).isEmpty();
    }
}
