package com.vetsoftware.app.state.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.out.CountryQueryPort;
import com.vetsoftware.app.state.application.port.out.StateRepository;
import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
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
@DisplayName("CreateStateService")
class CreateStateServiceTest {

    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Mock
    private StateRepository repository;
    @Mock
    private CountryQueryPort countryQueryPort;

    @InjectMocks
    private CreateStateService service;

    @Captor
    private ArgumentCaptor<State> stateCaptor;

    @Test
    @DisplayName("persiste el departamento con el pais resuelto por el puerto")
    void persiste_el_departamento_con_el_pais_resuelto() {
        when(countryQueryPort.findById(1L)).thenReturn(Optional.of(COLOMBIA));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new CreateStateCommand("Antioquia", 1L, "05"));

        verify(repository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getName()).isEqualTo("Antioquia");
        assertThat(stateCaptor.getValue().getCountry()).isEqualTo(COLOMBIA);
        assertThat(stateCaptor.getValue().getDaneCode()).isEqualTo("05");
        assertThat(stateCaptor.getValue().getId()).isNull();
        assertThat(stateCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("devuelve el dto del departamento ya persistido, con el id que asigno la base")
    void devuelve_el_dto_con_el_id_asignado() {
        LocalDateTime creacion = LocalDateTime.of(2026, 1, 15, 10, 30);
        when(countryQueryPort.findById(1L)).thenReturn(Optional.of(COLOMBIA));
        when(repository.save(any()))
                .thenReturn(new State(9L, "Antioquia", COLOMBIA, "05", creacion, true));

        StateDto dto = service.execute(new CreateStateCommand("Antioquia", 1L, "05"));

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Antioquia");
        assertThat(dto.country().id()).isEqualTo(1L);
        assertThat(dto.daneCode()).isEqualTo("05");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un pais inexistente no llega al dominio ni al repositorio")
    void pais_inexistente_no_escribe_nada() {
        when(countryQueryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new CreateStateCommand("Antioquia", 99L, "05")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country not found: 99");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("un nombre invalido revienta en el dominio y no escribe nada")
    void nombre_invalido_no_escribe_nada() {
        when(countryQueryPort.findById(1L)).thenReturn(Optional.of(COLOMBIA));

        assertThatThrownBy(() -> service.execute(new CreateStateCommand("", 1L, "05")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        verifyNoInteractions(repository);
    }
}
