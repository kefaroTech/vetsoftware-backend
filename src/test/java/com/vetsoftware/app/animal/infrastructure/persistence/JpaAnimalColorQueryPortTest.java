package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalColorQueryPort — adaptador sobre AnimalColorJpaRepository")
class JpaAnimalColorQueryPortTest {

    @Mock
    private AnimalColorJpaRepository animalColorJpaRepository;
    @Mock
    private AnimalColorJpaEntity colorEntity;
    @InjectMocks
    private JpaAnimalColorQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea el color encontrado a su companion VO")
        void mapea_el_color_encontrado_a_su_companion_vo() {
            when(colorEntity.getId()).thenReturn(AnimalMother.NEGRO.id());
            when(colorEntity.getName()).thenReturn(AnimalMother.NEGRO.name());
            when(animalColorJpaRepository.findById(AnimalMother.NEGRO.id()))
                    .thenReturn(Optional.of(colorEntity));

            Optional<AnimalColorRef> resultado = port.findById(AnimalMother.NEGRO.id());

            assertThat(resultado).contains(AnimalMother.NEGRO);
        }

        @Test
        @DisplayName("un color inexistente devuelve vacio")
        void un_color_inexistente_devuelve_vacio() {
            when(animalColorJpaRepository.findById(AnimalMother.NEGRO.id()))
                    .thenReturn(Optional.empty());

            Optional<AnimalColorRef> resultado = port.findById(AnimalMother.NEGRO.id());

            assertThat(resultado).isEmpty();
        }
    }
}
