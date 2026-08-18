package com.vetsoftware.app.breed.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalChildrenQueryPort (breed) — animales activos de una raza")
class JpaAnimalChildrenQueryPortTest {

    @Mock
    private AnimalJpaRepository jpaRepository;
    @InjectMocks
    private JpaAnimalChildrenQueryPort port;

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("delega en existsByBreed_Id y devuelve true si hay animales activos")
        void delega_y_devuelve_true_si_hay_animales_activos() {
            when(jpaRepository.existsByBreed_Id(7L)).thenReturn(true);

            assertThat(port.existsActiveByBreedId(7L)).isTrue();
        }

        @Test
        @DisplayName("devuelve false si no hay animales activos de esa raza")
        void devuelve_false_si_no_hay_animales_activos() {
            when(jpaRepository.existsByBreed_Id(7L)).thenReturn(false);

            assertThat(port.existsActiveByBreedId(7L)).isFalse();
        }
    }
}
