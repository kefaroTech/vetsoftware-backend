package com.vetsoftware.app.country.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
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
@DisplayName("UpdateCountryService")
class UpdateCountryServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Mock
    private CountryRepository repository;

    @InjectMocks
    private UpdateCountryService service;

    @Captor
    private ArgumentCaptor<Country> countryCaptor;

    private static Country colombia() {
        return new Country(7L, "Colombia", CREACION, true);
    }

    @Test
    @DisplayName("guarda el pais con el nombre nuevo y sin tocar la fecha de creacion")
    void guarda_el_nombre_nuevo() {
        when(repository.findById(7L)).thenReturn(Optional.of(colombia()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new UpdateCountryCommand(7L, "Republica de Colombia"));

        verify(repository).save(countryCaptor.capture());
        assertThat(countryCaptor.getValue().getName()).isEqualTo("Republica de Colombia");
        assertThat(countryCaptor.getValue().getId()).isEqualTo(7L);
        assertThat(countryCaptor.getValue().getCreatedDate()).isEqualTo(CREACION);
    }

    @Test
    @DisplayName("devuelve el dto del pais actualizado")
    void devuelve_el_dto_actualizado() {
        when(repository.findById(7L)).thenReturn(Optional.of(colombia()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CountryDto dto = service.execute(new UpdateCountryCommand(7L, "Republica de Colombia"));

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Republica de Colombia");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("si el pais no existe lanza 404 de dominio y no guarda nada")
    void pais_inexistente_no_guarda_nada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new UpdateCountryCommand(99L, "Chile")))
                .isInstanceOf(CountryNotFoundException.class)
                .hasMessageContaining("Country not found: 99");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un nombre vacio rechaza la actualizacion y no guarda nada")
    void nombre_vacio_no_guarda_nada() {
        when(repository.findById(7L)).thenReturn(Optional.of(colombia()));

        assertThatThrownBy(() -> service.execute(new UpdateCountryCommand(7L, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un nombre de mas de 100 caracteres rechaza la actualizacion")
    void nombre_demasiado_largo_no_guarda_nada() {
        when(repository.findById(7L)).thenReturn(Optional.of(colombia()));

        assertThatThrownBy(() -> service.execute(new UpdateCountryCommand(7L, "C".repeat(101))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be 100 chars or less");

        verify(repository, never()).save(any());
    }
}
