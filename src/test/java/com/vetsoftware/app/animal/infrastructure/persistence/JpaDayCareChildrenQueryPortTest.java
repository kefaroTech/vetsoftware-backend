package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.daycare.infrastructure.persistence.DayCareJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDayCareChildrenQueryPort")
class JpaDayCareChildrenQueryPortTest {

    @Mock
    private DayCareJpaRepository jpaRepository;
    @InjectMocks
    private JpaDayCareChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByAnimal_Id cuando hay estancias de guarderia activas")
    void delega_cuando_hay_estancias_activas() {
        when(jpaRepository.existsByAnimal_Id(100L)).thenReturn(true);

        assertThat(port.existsActiveByAnimalId(100L)).isTrue();
    }

    @Test
    @DisplayName("delega en existsByAnimal_Id cuando no hay estancias de guarderia activas")
    void delega_cuando_no_hay_estancias_activas() {
        when(jpaRepository.existsByAnimal_Id(100L)).thenReturn(false);

        assertThat(port.existsActiveByAnimalId(100L)).isFalse();
    }
}
