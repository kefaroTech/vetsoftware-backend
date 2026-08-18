package com.vetsoftware.app.deworming.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.deworming.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de resolucion de animal para desparasitaciones. Acotado por
 * empresa: es la barrera que impide colgar una desparasitacion de esta empresa
 * del animal de otro tenant.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort — deworming")
class JpaAnimalQueryPortTest {

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @InjectMocks
    private JpaAnimalQueryPort port;

    private static AnimalJpaEntity animalEncontrado(long id, String name, String code) {
        AnimalJpaEntity entity = mock(AnimalJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getName()).thenReturn(name);
        when(entity.getCode()).thenReturn(code);
        return entity;
    }

    @Test
    @DisplayName("mapea el animal de la empresa a su companion VO")
    void mapea_el_animal_encontrado_a_su_companion_vo() {
        AnimalJpaEntity animal = animalEncontrado(3L, "Firulais", "A-001");
        when(animalJpaRepository.findByIdAndCompany_Id(3L, 9L)).thenReturn(Optional.of(animal));

        Optional<AnimalRef> ref = port.findByIdAndCompanyId(3L, 9L);

        assertThat(ref).contains(new AnimalRef(3L, "Firulais", "A-001"));
    }

    @Test
    @DisplayName("devuelve vacio si el animal no existe")
    void devuelve_vacio_si_el_animal_no_existe() {
        when(animalJpaRepository.findByIdAndCompany_Id(99L, 9L)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(99L, 9L)).isEmpty();
    }

    @Test
    @DisplayName("devuelve vacio si el animal es de otra empresa")
    void devuelve_vacio_si_el_animal_es_de_otra_empresa() {
        when(animalJpaRepository.findByIdAndCompany_Id(3L, 77L)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(3L, 77L)).isEmpty();
    }
}
