package com.vetsoftware.app.breed.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
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
@DisplayName("ReactivateBreedService")
class ReactivateBreedServiceTest {

    @Mock
    private BreedRepository repository;
    @InjectMocks
    private ReactivateBreedService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la raza releida")
        void reactiva_y_devuelve_la_raza_releida() {
            when(repository.reactivate(BreedMother.BREED_ID)).thenReturn(1);
            when(repository.findById(BreedMother.BREED_ID))
                    .thenReturn(Optional.of(BreedMother.labrador()));

            BreedDto dto = service.execute(BreedMother.BREED_ID);

            assertThat(dto.id()).isEqualTo(BreedMother.BREED_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BreedNotFoundException si no reactivo ninguna fila y no relee")
        void lanza_breed_not_found_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(BreedMother.BREED_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(BreedMother.BREED_ID))
                    .isInstanceOf(BreedNotFoundException.class)
                    .hasMessageContaining("Breed not found: " + BreedMother.BREED_ID);

            verify(repository, never()).findById(BreedMother.BREED_ID);
        }
    }
}
