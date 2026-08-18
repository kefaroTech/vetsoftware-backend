package com.vetsoftware.app.state.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.state.domain.CountryRef;
import com.vetsoftware.app.state.domain.State;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StateDto — proyeccion del agregado")
class StateDtoTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CountryRef COLOMBIA = new CountryRef(1L, "Colombia");

    @Test
    @DisplayName("from copia los campos propios y proyecta el pais a su summary")
    void from_copia_los_campos_y_proyecta_el_pais() {
        State state = new State(7L, "Antioquia", COLOMBIA, "05", CREACION, true);

        StateDto dto = StateDto.from(state);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Antioquia");
        assertThat(dto.country()).isEqualTo(new CountrySummaryDto(1L, "Colombia"));
        assertThat(dto.daneCode()).isEqualTo("05");
        assertThat(dto.createdDate()).isEqualTo(CREACION);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from propaga el id nulo de un departamento recien creado")
    void from_propaga_el_id_nulo() {
        State state = new State(null, "Antioquia", COLOMBIA, "05", CREACION, true);

        assertThat(StateDto.from(state).id()).isNull();
    }

    @Test
    @DisplayName("from refleja el estado deshabilitado")
    void from_refleja_el_estado_deshabilitado() {
        State state = new State(7L, "Antioquia", COLOMBIA, "05", CREACION, false);

        assertThat(StateDto.from(state).enabled()).isFalse();
    }

    @Test
    @DisplayName("from propaga un codigo dane nulo")
    void from_propaga_dane_code_nulo() {
        State state = new State(7L, "Antioquia", COLOMBIA, null, CREACION, true);

        assertThat(StateDto.from(state).daneCode()).isNull();
    }
}
