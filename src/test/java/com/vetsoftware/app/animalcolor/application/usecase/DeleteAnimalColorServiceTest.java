package com.vetsoftware.app.animalcolor.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColorHasActiveChildrenException;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import com.vetsoftware.app.animalcolor.testsupport.AnimalColorMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAnimalColorService")
class DeleteAnimalColorServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @Mock
    private AnimalChildrenQueryPort animalChildrenQueryPort;
    @InjectMocks
    private DeleteAnimalColorService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina el color cuando no tiene animales activos")
        void elimina_el_color_cuando_no_tiene_animales_activos() {
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.of(AnimalColorMother.negro()));
            when(animalChildrenQueryPort
                    .existsActiveByAnimalColorId(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(false);

            service.execute(AnimalColorMother.ANIMAL_COLOR_ID);

            verify(repository).delete(AnimalColorMother.ANIMAL_COLOR_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza AnimalColorNotFoundException si el color no existe y no consulta animales")
        void lanza_animal_color_not_found_si_el_color_no_existe() {
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalColorMother.ANIMAL_COLOR_ID))
                    .isInstanceOf(AnimalColorNotFoundException.class).hasMessageContaining(
                            "AnimalColor not found: " + AnimalColorMother.ANIMAL_COLOR_ID);

            verifyNoInteractions(animalChildrenQueryPort);
        }

        @Test
        @DisplayName("no elimina si el color tiene animales activos")
        void no_elimina_si_el_color_tiene_animales_activos() {
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.of(AnimalColorMother.negro()));
            when(animalChildrenQueryPort
                    .existsActiveByAnimalColorId(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(AnimalColorMother.ANIMAL_COLOR_ID))
                    .isInstanceOf(AnimalColorHasActiveChildrenException.class)
                    .hasMessageContaining("animal");

            verify(repository, never()).delete(AnimalColorMother.ANIMAL_COLOR_ID);
        }
    }
}
