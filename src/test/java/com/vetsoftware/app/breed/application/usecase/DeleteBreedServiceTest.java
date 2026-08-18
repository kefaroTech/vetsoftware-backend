package com.vetsoftware.app.breed.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.breed.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.domain.BreedHasActiveChildrenException;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.breed.testsupport.BreedMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteBreedService")
class DeleteBreedServiceTest {

    @Mock
    private BreedRepository repository;
    @Mock
    private AnimalChildrenQueryPort animalChildrenQueryPort;
    @InjectMocks
    private DeleteBreedService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina la raza cuando no tiene animales activos")
        void elimina_la_raza_cuando_no_tiene_animales_activos() {
            when(repository.findById(BreedMother.BREED_ID))
                    .thenReturn(Optional.of(BreedMother.labrador()));
            when(animalChildrenQueryPort.existsActiveByBreedId(BreedMother.BREED_ID))
                    .thenReturn(false);

            service.execute(BreedMother.BREED_ID);

            verify(repository).delete(BreedMother.BREED_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BreedNotFoundException si la raza no existe y no consulta animales")
        void lanza_breed_not_found_si_la_raza_no_existe() {
            when(repository.findById(BreedMother.BREED_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BreedMother.BREED_ID))
                    .isInstanceOf(BreedNotFoundException.class)
                    .hasMessageContaining("Breed not found: " + BreedMother.BREED_ID);

            verifyNoInteractions(animalChildrenQueryPort);
        }

        @Test
        @DisplayName("no elimina si la raza tiene animales activos")
        void no_elimina_si_la_raza_tiene_animales_activos() {
            when(repository.findById(BreedMother.BREED_ID))
                    .thenReturn(Optional.of(BreedMother.labrador()));
            when(animalChildrenQueryPort.existsActiveByBreedId(BreedMother.BREED_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(BreedMother.BREED_ID))
                    .isInstanceOf(BreedHasActiveChildrenException.class)
                    .hasMessageContaining("animal");

            verify(repository, never()).delete(BreedMother.BREED_ID);
        }
    }
}
