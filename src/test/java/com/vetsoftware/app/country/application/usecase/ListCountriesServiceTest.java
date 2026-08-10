package com.vetsoftware.app.country.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCountriesService")
class ListCountriesServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Mock
    private CountryRepository repository;

    @InjectMocks
    private ListCountriesService service;

    @Test
    @DisplayName("proyecta cada pais a su dto respetando el orden del repositorio")
    void proyecta_cada_pais_respetando_el_orden() {
        when(repository.findAll()).thenReturn(List.of(new Country(1L, "Colombia", CREACION, true),
                new Country(2L, "Chile", CREACION, false)));

        List<CountryDto> dtos = service.listAll();

        assertThat(dtos).extracting(CountryDto::id, CountryDto::name, CountryDto::enabled)
                .containsExactly(tuple(1L, "Colombia", true), tuple(2L, "Chile", false));
    }

    @Test
    @DisplayName("sin paises devuelve una lista vacia, nunca null")
    void sin_paises_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
