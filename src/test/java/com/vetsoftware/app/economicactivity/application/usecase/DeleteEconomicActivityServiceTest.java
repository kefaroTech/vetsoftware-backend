package com.vetsoftware.app.economicactivity.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import com.vetsoftware.app.economicactivity.testsupport.EconomicActivityMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteEconomicActivityService")
class DeleteEconomicActivityServiceTest {

    @Mock
    private EconomicActivityRepository repository;

    private DeleteEconomicActivityService service;

    @BeforeEach
    void setUp() {
        service = new DeleteEconomicActivityService(repository);
    }

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("borra la actividad existente")
        void borra_la_actividad_existente() {
            when(repository.findById(EconomicActivityMother.ECONOMIC_ACTIVITY_ID))
                    .thenReturn(Optional.of(EconomicActivityMother.existente()));

            service.execute(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);

            verify(repository).delete(EconomicActivityMother.ECONOMIC_ACTIVITY_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no borra nada si la actividad no existe")
        void no_borra_nada_si_la_actividad_no_existe() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L))
                    .isInstanceOf(EconomicActivityNotFoundException.class)
                    .hasMessageContaining("EconomicActivity not found: 99");

            verify(repository, never()).delete(anyLong());
        }
    }
}
