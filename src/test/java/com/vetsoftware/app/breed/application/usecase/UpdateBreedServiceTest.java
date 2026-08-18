package com.vetsoftware.app.breed.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.breed.testsupport.BreedMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateBreedService")
class UpdateBreedServiceTest {

    @Mock
    private BreedRepository repository;
    @Mock
    private SpecieQueryPort specieQueryPort;
    @InjectMocks
    private UpdateBreedService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza nombre y especie y persiste el agregado")
        void actualiza_nombre_y_especie_y_persiste_el_agregado() {
            Breed existente = BreedMother.labrador();
            when(repository.findById(BreedMother.BREED_ID)).thenReturn(Optional.of(existente));
            when(specieQueryPort.findById(BreedMother.GATO.id()))
                    .thenReturn(Optional.of(BreedMother.GATO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BreedDto dto = service.execute(BreedMother.comandoActualizar());

            ArgumentCaptor<Breed> captor = ArgumentCaptor.forClass(Breed.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Golden");
            assertThat(captor.getValue().getSpecie()).isEqualTo(BreedMother.GATO);
            assertThat(dto.name()).isEqualTo("Golden");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BreedNotFoundException si la raza no existe y no consulta la especie")
        void lanza_breed_not_found_si_la_raza_no_existe() {
            when(repository.findById(BreedMother.BREED_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BreedMother.comandoActualizar()))
                    .isInstanceOf(BreedNotFoundException.class)
                    .hasMessageContaining("Breed not found: " + BreedMother.BREED_ID);

            verifyNoInteractions(specieQueryPort);
        }

        @Test
        @DisplayName("no guarda si la especie destino no existe")
        void no_guarda_si_la_especie_destino_no_existe() {
            when(repository.findById(BreedMother.BREED_ID))
                    .thenReturn(Optional.of(BreedMother.labrador()));
            when(specieQueryPort.findById(BreedMother.GATO.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BreedMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + BreedMother.GATO.id());

            verify(repository, never()).save(any());
        }
    }
}
