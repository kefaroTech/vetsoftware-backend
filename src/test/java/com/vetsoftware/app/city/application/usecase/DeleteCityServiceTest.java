package com.vetsoftware.app.city.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.city.application.port.out.CityRepository;
import com.vetsoftware.app.city.application.port.out.OwnerChildrenQueryPort;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.CityHasActiveChildrenException;
import com.vetsoftware.app.city.domain.CityNotFoundException;
import com.vetsoftware.app.city.testsupport.CityMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCityService")
class DeleteCityServiceTest {

    @Mock
    private CityRepository repository;
    @Mock
    private OwnerChildrenQueryPort ownerChildrenQueryPort;

    private DeleteCityService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteCityService(repository, ownerChildrenQueryPort);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la ciudad cuando no tiene propietarios activos")
        void borra_la_ciudad_sin_propietarios_activos() {
            City existente = CityMother.activa();
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.of(existente));
            when(ownerChildrenQueryPort.existsActiveByCityId(CityMother.CITY_ID)).thenReturn(false);

            service.execute(CityMother.CITY_ID);

            verify(repository).delete(CityMother.CITY_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no consulta propietarios ni borra si la ciudad no existe")
        void no_consulta_propietarios_ni_borra_si_la_ciudad_no_existe() {
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(CityMother.CITY_ID))
                    .isInstanceOf(CityNotFoundException.class)
                    .hasMessageContaining("City not found: " + CityMother.CITY_ID);

            verifyNoInteractions(ownerChildrenQueryPort);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("no borra si tiene propietarios activos")
        void no_borra_si_tiene_propietarios_activos() {
            City existente = CityMother.activa();
            when(repository.findById(CityMother.CITY_ID)).thenReturn(Optional.of(existente));
            when(ownerChildrenQueryPort.existsActiveByCityId(CityMother.CITY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(CityMother.CITY_ID))
                    .isInstanceOf(CityHasActiveChildrenException.class)
                    .hasMessageContaining("" + CityMother.CITY_ID).hasMessageContaining("owner");

            verify(repository, never()).delete(any());
        }
    }
}
