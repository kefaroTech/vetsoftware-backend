package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSpecieQueryPort — adaptador sobre SpecieJpaRepository")
class JpaSpecieQueryPortTest {

    @Mock
    private SpecieJpaRepository specieJpaRepository;
    @Mock
    private SpecieJpaEntity specieEntity;
    @InjectMocks
    private JpaSpecieQueryPort port;

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la especie encontrada a su companion VO")
        void mapea_la_especie_encontrada_a_su_companion_vo() {
            when(specieEntity.getId()).thenReturn(AnimalMother.PERRO.id());
            when(specieEntity.getName()).thenReturn(AnimalMother.PERRO.name());
            when(specieJpaRepository.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.of(specieEntity));

            Optional<SpecieRef> resultado = port.findById(AnimalMother.PERRO.id());

            assertThat(resultado).contains(AnimalMother.PERRO);
        }

        @Test
        @DisplayName("una especie inexistente devuelve vacio")
        void una_especie_inexistente_devuelve_vacio() {
            when(specieJpaRepository.findById(AnimalMother.PERRO.id()))
                    .thenReturn(Optional.empty());

            Optional<SpecieRef> resultado = port.findById(AnimalMother.PERRO.id());

            assertThat(resultado).isEmpty();
        }
    }
}
