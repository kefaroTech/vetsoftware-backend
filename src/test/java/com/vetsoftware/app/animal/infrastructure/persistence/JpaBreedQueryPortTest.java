package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaBreedQueryPort — adaptador sobre BreedJpaRepository")
class JpaBreedQueryPortTest {

    @Mock
    private BreedJpaRepository breedJpaRepository;
    @Mock
    private BreedJpaEntity breedEntity;
    @InjectMocks
    private JpaBreedQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la raza encontrada a su companion VO")
        void mapea_la_raza_encontrada_a_su_companion_vo() {
            when(breedEntity.getId()).thenReturn(AnimalMother.LABRADOR.id());
            when(breedEntity.getName()).thenReturn(AnimalMother.LABRADOR.name());
            when(breedJpaRepository.findById(AnimalMother.LABRADOR.id()))
                    .thenReturn(Optional.of(breedEntity));

            Optional<BreedRef> resultado = port.findById(AnimalMother.LABRADOR.id());

            assertThat(resultado).contains(AnimalMother.LABRADOR);
        }

        @Test
        @DisplayName("una raza inexistente devuelve vacio")
        void una_raza_inexistente_devuelve_vacio() {
            when(breedJpaRepository.findById(AnimalMother.LABRADOR.id()))
                    .thenReturn(Optional.empty());

            Optional<BreedRef> resultado = port.findById(AnimalMother.LABRADOR.id());

            assertThat(resultado).isEmpty();
        }
    }
}
