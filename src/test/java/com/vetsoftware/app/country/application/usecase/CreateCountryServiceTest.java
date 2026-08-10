package com.vetsoftware.app.country.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCountryService")
class CreateCountryServiceTest {

    @Mock
    private CountryRepository repository;

    @InjectMocks
    private CreateCountryService service;

    @Captor
    private ArgumentCaptor<Country> countryCaptor;

    @Test
    @DisplayName("persiste un pais habilitado con el nombre del comando")
    void persiste_un_pais_habilitado() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new CreateCountryCommand("Colombia"));

        verify(repository).save(countryCaptor.capture());
        assertThat(countryCaptor.getValue().getName()).isEqualTo("Colombia");
        assertThat(countryCaptor.getValue().getId()).isNull();
        assertThat(countryCaptor.getValue().isEnabled()).isTrue();
        assertThat(countryCaptor.getValue().getCreatedDate()).isNotNull();
    }

    @Test
    @DisplayName("devuelve el dto del pais ya persistido, con el id que asigno la base")
    void devuelve_el_dto_con_el_id_asignado() {
        LocalDateTime creacion = LocalDateTime.of(2026, 1, 15, 10, 30);
        when(repository.save(any())).thenReturn(new Country(9L, "Colombia", creacion, true));

        CountryDto dto = service.execute(new CreateCountryCommand("Colombia"));

        assertThat(dto.id()).isEqualTo(9L);
        assertThat(dto.name()).isEqualTo("Colombia");
        assertThat(dto.createdDate()).isEqualTo(creacion);
        assertThat(dto.enabled()).isTrue();
    }

    @ParameterizedTest(name = "nombre = \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("un nombre invalido revienta en el dominio y no escribe nada")
    void nombre_invalido_no_escribe_nada(String nombre) {
        assertThatThrownBy(() -> service.execute(new CreateCountryCommand(nombre)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("un nombre de mas de 100 caracteres no llega al repositorio")
    void nombre_demasiado_largo_no_escribe_nada() {
        assertThatThrownBy(() -> service.execute(new CreateCountryCommand("C".repeat(101))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be 100 chars or less");

        verifyNoInteractions(repository);
    }
}
