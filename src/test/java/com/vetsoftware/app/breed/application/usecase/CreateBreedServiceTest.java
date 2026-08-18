package com.vetsoftware.app.breed.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.port.out.BreedRepository;
import com.vetsoftware.app.breed.application.port.out.SpecieQueryPort;
import com.vetsoftware.app.breed.domain.Breed;
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
@DisplayName("CreateBreedService")
class CreateBreedServiceTest {

    @Mock
    private BreedRepository repository;
    @Mock
    private SpecieQueryPort specieQueryPort;
    @InjectMocks
    private CreateBreedService service;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la raza con la especie resuelta por el puerto")
        void persiste_la_raza_con_la_especie_resuelta() {
            when(specieQueryPort.findById(BreedMother.PERRO.id()))
                    .thenReturn(Optional.of(BreedMother.PERRO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BreedDto dto = service.execute(BreedMother.comandoCrear());

            ArgumentCaptor<Breed> captor = ArgumentCaptor.forClass(Breed.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Labrador");
            assertThat(captor.getValue().getSpecie()).isEqualTo(BreedMother.PERRO);
            assertThat(dto.name()).isEqualTo("Labrador");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio si la especie no existe")
        void no_toca_el_repositorio_si_la_especie_no_existe() {
            when(specieQueryPort.findById(BreedMother.PERRO.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(BreedMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Specie not found: " + BreedMother.PERRO.id());

            verifyNoInteractions(repository);
        }
    }
}
