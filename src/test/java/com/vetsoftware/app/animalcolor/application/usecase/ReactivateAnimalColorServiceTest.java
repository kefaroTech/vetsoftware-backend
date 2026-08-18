package com.vetsoftware.app.animalcolor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
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
@DisplayName("ReactivateAnimalColorService")
class ReactivateAnimalColorServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @InjectMocks
    private ReactivateAnimalColorService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el color releido")
        void reactiva_y_devuelve_el_color_releido() {
            when(repository.reactivate(AnimalColorMother.ANIMAL_COLOR_ID)).thenReturn(1);
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.of(AnimalColorMother.negro()));

            AnimalColorDto dto = service.execute(AnimalColorMother.ANIMAL_COLOR_ID);

            assertThat(dto.id()).isEqualTo(AnimalColorMother.ANIMAL_COLOR_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza AnimalColorNotFoundException si no reactivo ninguna fila y no relee")
        void lanza_animal_color_not_found_si_no_reactivo_ninguna_fila() {
            when(repository.reactivate(AnimalColorMother.ANIMAL_COLOR_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(AnimalColorMother.ANIMAL_COLOR_ID))
                    .isInstanceOf(AnimalColorNotFoundException.class).hasMessageContaining(
                            "AnimalColor not found: " + AnimalColorMother.ANIMAL_COLOR_ID);

            verify(repository, never()).findById(AnimalColorMother.ANIMAL_COLOR_ID);
        }
    }
}
