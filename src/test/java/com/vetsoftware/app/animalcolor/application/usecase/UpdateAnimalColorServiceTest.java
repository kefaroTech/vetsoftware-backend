package com.vetsoftware.app.animalcolor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import com.vetsoftware.app.animalcolor.testsupport.AnimalColorMother;
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
@DisplayName("UpdateAnimalColorService")
class UpdateAnimalColorServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @Mock
    private SpecieQueryPort specieQueryPort;
    @InjectMocks
    private UpdateAnimalColorService service;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza nombre y especie y persiste el agregado")
        void actualiza_nombre_y_especie_y_persiste_el_agregado() {
            AnimalColor existente = AnimalColorMother.negro();
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.of(existente));
            when(specieQueryPort.findById(AnimalColorMother.GATO.id()))
                    .thenReturn(Optional.of(AnimalColorMother.GATO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnimalColorDto dto = service.execute(AnimalColorMother.comandoActualizar());

            ArgumentCaptor<AnimalColor> captor = ArgumentCaptor.forClass(AnimalColor.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Blanco");
            assertThat(captor.getValue().getSpecie()).isEqualTo(AnimalColorMother.GATO);
            assertThat(dto.name()).isEqualTo("Blanco");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza AnimalColorNotFoundException si el color no existe y no consulta la especie")
        void lanza_animal_color_not_found_si_el_color_no_existe() {
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalColorMother.comandoActualizar()))
                    .isInstanceOf(AnimalColorNotFoundException.class).hasMessageContaining(
                            "AnimalColor not found: " + AnimalColorMother.ANIMAL_COLOR_ID);

            verifyNoInteractions(specieQueryPort);
        }

        @Test
        @DisplayName("no guarda si la especie destino no existe")
        void no_guarda_si_la_especie_destino_no_existe() {
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.of(AnimalColorMother.negro()));
            when(specieQueryPort.findById(AnimalColorMother.GATO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalColorMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + AnimalColorMother.GATO.id());

            verify(repository, never()).save(any());
        }
    }
}
