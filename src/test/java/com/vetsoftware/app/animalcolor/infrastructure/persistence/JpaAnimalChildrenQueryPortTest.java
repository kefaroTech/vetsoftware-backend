package com.vetsoftware.app.animalcolor.infrastructure.persistence;

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
@DisplayName("JpaAnimalChildrenQueryPort (animalcolor) — animales activos de un color")
class JpaAnimalChildrenQueryPortTest {

    @Mock
    private AnimalJpaRepository jpaRepository;
    @InjectMocks
    private JpaAnimalChildrenQueryPort port;

    @Nested
    @DisplayName("consulta")
    class Consulta {

        @Test
        @DisplayName("delega en existsByColor_Id y devuelve true si hay animales activos")
        void delega_y_devuelve_true_si_hay_animales_activos() {
            when(jpaRepository.existsByColor_Id(7L)).thenReturn(true);

            assertThat(port.existsActiveByAnimalColorId(7L)).isTrue();
        }

        @Test
        @DisplayName("devuelve false si no hay animales activos de ese color")
        void devuelve_false_si_no_hay_animales_activos() {
            when(jpaRepository.existsByColor_Id(7L)).thenReturn(false);

            assertThat(port.existsActiveByAnimalColorId(7L)).isFalse();
        }
    }
}
