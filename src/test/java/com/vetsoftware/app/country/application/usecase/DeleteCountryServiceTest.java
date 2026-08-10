package com.vetsoftware.app.country.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.country.application.port.out.CountryRepository;
import com.vetsoftware.app.country.application.port.out.StateChildrenQueryPort;
import com.vetsoftware.app.country.domain.Country;
import com.vetsoftware.app.country.domain.CountryHasActiveChildrenException;
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
@DisplayName("DeleteCountryService")
class DeleteCountryServiceTest {

    private static final LocalDateTime CREACION = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Mock
    private CountryRepository repository;
    @Mock
    private StateChildrenQueryPort stateChildrenQueryPort;

    @InjectMocks
    private DeleteCountryService service;

    private static Country colombia() {
        return new Country(7L, "Colombia", CREACION, true);
    }

    @Test
    @DisplayName("borra el pais cuando existe y no tiene departamentos activos")
    void borra_el_pais_sin_hijos_activos() {
        when(repository.findById(7L)).thenReturn(Optional.of(colombia()));
        when(stateChildrenQueryPort.existsActiveByCountryId(7L)).thenReturn(false);

        service.execute(7L);

        verify(repository).delete(7L);
    }

    @Test
    @DisplayName("un pais inexistente no se borra ni se consulta por hijos")
    void pais_inexistente_no_borra_nada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L)).isInstanceOf(CountryNotFoundException.class)
                .hasMessageContaining("Country not found: 99");

        verify(repository, never()).delete(any());
        verifyNoInteractions(stateChildrenQueryPort);
    }

    @Test
    @DisplayName("con departamentos activos rechaza el borrado y no escribe")
    void con_hijos_activos_no_borra() {
        when(repository.findById(7L)).thenReturn(Optional.of(colombia()));
        when(stateChildrenQueryPort.existsActiveByCountryId(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(7L))
                .isInstanceOf(CountryHasActiveChildrenException.class).hasMessageContaining("7")
                .hasMessageContaining("state");

        verify(repository, never()).delete(any());
    }
}
