package com.vetsoftware.app.breed.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindBreedService")
class FindBreedServiceTest {

    @Mock
    private BreedRepository repository;
    @InjectMocks
    private FindBreedService service;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve la raza encontrada mapeada a dto")
        void devuelve_la_raza_encontrada_mapeada_a_dto() {
            when(repository.findById(BreedMother.BREED_ID))
                    .thenReturn(Optional.of(BreedMother.labrador()));

            BreedDto dto = service.findById(BreedMother.BREED_ID);

            assertThat(dto.id()).isEqualTo(BreedMother.BREED_ID);
            assertThat(dto.name()).isEqualTo("Labrador");
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza BreedNotFoundException si la raza no existe")
        void lanza_breed_not_found_si_la_raza_no_existe() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(999L))
                    .isInstanceOf(BreedNotFoundException.class)
                    .hasMessageContaining("Breed not found: 999");
        }
    }
}
