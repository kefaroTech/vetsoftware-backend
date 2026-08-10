package com.vetsoftware.app.country.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.domain.Country;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCountryService")
class FindCountryServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Mock
    private CountryRepository repository;

    @InjectMocks
    private FindCountryService service;

    @Test
    @DisplayName("devuelve el dto del pais existente campo por campo")
    void devuelve_el_dto_del_pais_existente() {
        when(repository.findById(7L))
                .thenReturn(Optional.of(new Country(7L, "Colombia", CREACION, true)));

        CountryDto dto = service.findById(7L);

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.name()).isEqualTo("Colombia");
        assertThat(dto.createdDate()).isEqualTo(CREACION);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un pais deshabilitado se devuelve marcado como tal")
    void devuelve_el_pais_deshabilitado() {
        when(repository.findById(7L))
                .thenReturn(Optional.of(new Country(7L, "Colombia", CREACION, false)));

        assertThat(service.findById(7L).enabled()).isFalse();
    }

    @Test
    @DisplayName("si no existe lanza CountryNotFoundException con el id buscado")
    void pais_inexistente_lanza_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(CountryNotFoundException.class)
                .hasMessageContaining("Country not found: 99");
    }
}
