package com.vetsoftware.app.consultation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.consultation.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    @DisplayName("mapea el animal encontrado a su companion VO, acotado por empresa")
    void mapea_el_animal_encontrado_a_su_companion_vo() {
        AnimalJpaEntity animal = animalEncontrado(100L, "Firulais", "A-001");
        when(animalJpaRepository.findByIdAndCompany_Id(100L, 9L)).thenReturn(Optional.of(animal));

        Optional<AnimalRef> ref = port.findByIdAndCompanyId(100L, 9L);

        assertThat(ref).contains(new AnimalRef(100L, "Firulais", "A-001"));
    }

    @Test
    @DisplayName("devuelve vacio si el animal no existe en esa empresa")
    void devuelve_vacio_si_el_animal_no_existe_en_esa_empresa() {
        when(animalJpaRepository.findByIdAndCompany_Id(100L, 9L)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(100L, 9L)).isEmpty();
    }
}
