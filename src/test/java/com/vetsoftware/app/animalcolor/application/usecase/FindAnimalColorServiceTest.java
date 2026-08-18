package com.vetsoftware.app.animalcolor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindAnimalColorService")
class FindAnimalColorServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @InjectMocks
    private FindAnimalColorService service;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el color encontrado mapeado a dto")
        void devuelve_el_color_encontrado_mapeado_a_dto() {
            when(repository.findById(AnimalColorMother.ANIMAL_COLOR_ID))
                    .thenReturn(Optional.of(AnimalColorMother.negro()));

            AnimalColorDto dto = service.findById(AnimalColorMother.ANIMAL_COLOR_ID);

            assertThat(dto.id()).isEqualTo(AnimalColorMother.ANIMAL_COLOR_ID);
            assertThat(dto.name()).isEqualTo("Negro");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza AnimalColorNotFoundException si el color no existe")
        void lanza_animal_color_not_found_si_el_color_no_existe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(AnimalColorNotFoundException.class)
                    .hasMessageContaining("AnimalColor not found: 999");
        }
    }
}
