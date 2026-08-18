package com.vetsoftware.app.specie.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalColorChildrenQueryPort (specie)")
class JpaAnimalColorChildrenQueryPortTest {

    @Mock
    private AnimalColorJpaRepository jpaRepository;
    @InjectMocks
    private JpaAnimalColorChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsBySpecie_Id cuando hay colores activos")
    void delega_cuando_hay_colores_activos() {
        when(jpaRepository.existsBySpecie_Id(100L)).thenReturn(true);

        assertThat(port.existsActiveBySpecieId(100L)).isTrue();
    }

    @Test
    @DisplayName("delega en existsBySpecie_Id cuando no hay colores activos")
    void delega_cuando_no_hay_colores_activos() {
        when(jpaRepository.existsBySpecie_Id(100L)).thenReturn(false);

        assertThat(port.existsActiveBySpecieId(100L)).isFalse();
    }
}
