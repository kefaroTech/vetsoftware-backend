package com.vetsoftware.app.animalcolor.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
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
@DisplayName("CreateAnimalColorService")
class CreateAnimalColorServiceTest {

    @Mock
    private AnimalColorRepository repository;
    @Mock
    private SpecieQueryPort specieQueryPort;
    @InjectMocks
    private CreateAnimalColorService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el color con la especie resuelta por el puerto")
        void persiste_el_color_con_la_especie_resuelta() {
            when(specieQueryPort.findById(AnimalColorMother.PERRO.id()))
                    .thenReturn(Optional.of(AnimalColorMother.PERRO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AnimalColorDto dto = service.execute(AnimalColorMother.comandoCrear());

            ArgumentCaptor<AnimalColor> captor = ArgumentCaptor.forClass(AnimalColor.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Negro");
            assertThat(captor.getValue().getSpecie()).isEqualTo(AnimalColorMother.PERRO);
            assertThat(dto.name()).isEqualTo("Negro");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio si la especie no existe")
        void no_toca_el_repositorio_si_la_especie_no_existe() {
            when(specieQueryPort.findById(AnimalColorMother.PERRO.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AnimalColorMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + AnimalColorMother.PERRO.id());

            verifyNoInteractions(repository);
        }
    }
}
