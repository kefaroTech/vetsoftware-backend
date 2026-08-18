package com.vetsoftware.app.animalcolor.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animalcolor.domain.SpecieRef;
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

/**
 * {@code SpecieJpaEntity} se mockea porque su constructor sin argumentos es
 * {@code protected} y no es instanciable desde este paquete. No tiene logica:
 * es un portador de datos, y mockearlo no oculta comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSpecieQueryPort (animalcolor) — resolucion de la especie referenciada")
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
        @DisplayName("mapea la entidad encontrada a SpecieRef")
        void mapea_la_entidad_encontrada_a_specie_ref() {
            when(specieEntity.getId()).thenReturn(1L);
            when(specieEntity.getName()).thenReturn("Perro");
            when(specieJpaRepository.findById(1L)).thenReturn(Optional.of(specieEntity));

            Optional<SpecieRef> ref = port.findById(1L);

            assertThat(ref).contains(new SpecieRef(1L, "Perro"));
        }

        @Test
        @DisplayName("devuelve vacio si la especie no existe")
        void devuelve_vacio_si_la_especie_no_existe() {
            when(specieJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
