package com.vetsoftware.app.vaccination.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.vaccination.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de resolucion de animal para vacunaciones. Consulta SIEMPRE el
 * finder acotado por empresa: el puerto no ofrece variante ancha.
 */
@ExtendWith(MockitoExtension.class)
class JpaAnimalQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

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
    @DisplayName("mapea el animal de mi empresa a su companion VO")
    void mapea_el_animal_encontrado_a_su_companion_vo() {
        AnimalJpaEntity animal = animalEncontrado(3L, "Firulais", "A-001");
        when(animalJpaRepository.findByIdAndCompany_Id(3L, EMPRESA))
                .thenReturn(Optional.of(animal));

        Optional<AnimalRef> ref = port.findByIdAndCompanyId(3L, EMPRESA);

        assertThat(ref).contains(new AnimalRef(3L, "Firulais", "A-001"));
    }

    @Test
    @DisplayName("el animal de otra empresa no existe para mi")
    void el_animal_de_otra_empresa_no_existe_para_mi() {
        when(animalJpaRepository.findByIdAndCompany_Id(3L, OTRA_EMPRESA))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(3L, OTRA_EMPRESA)).isEmpty();
    }
}
