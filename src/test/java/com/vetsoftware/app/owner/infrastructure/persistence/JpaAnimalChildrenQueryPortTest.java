package com.vetsoftware.app.owner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalChildrenQueryPort — animales activos de un owner")
class JpaAnimalChildrenQueryPortTest {

    @Mock
    private AnimalJpaRepository jpaRepository;

    @Test
    @DisplayName("delega en existsByOwner_Id con el id del owner")
    void delega_en_exists_by_owner_id() {
        JpaAnimalChildrenQueryPort adaptador = new JpaAnimalChildrenQueryPort(jpaRepository);
        when(jpaRepository.existsByOwner_Id(10L)).thenReturn(true);

        assertThat(adaptador.existsActiveByOwnerId(10L)).isTrue();
    }

    @Test
    @DisplayName("sin animales activos devuelve false")
    void sin_animales_activos_devuelve_false() {
        JpaAnimalChildrenQueryPort adaptador = new JpaAnimalChildrenQueryPort(jpaRepository);
        when(jpaRepository.existsByOwner_Id(10L)).thenReturn(false);

        assertThat(adaptador.existsActiveByOwnerId(10L)).isFalse();
    }
}
