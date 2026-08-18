package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort — adaptador sobre AnimalJpaRepository")
class JpaAnimalQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @Mock
    private AnimalJpaEntity animalEntity;
    @InjectMocks
    private JpaAnimalQueryPort port;

    @Nested
    @DisplayName("busqueda acotada por empresa")
    class Busqueda {

        @Test
        @DisplayName("mapea el animal de mi empresa a su companion VO")
        void mapea_el_animal_encontrado_a_su_companion_vo() {
            AnimalRef firulais = LaboratoryTestMother.FIRULAIS;
            when(animalEntity.getId()).thenReturn(firulais.id());
            when(animalEntity.getName()).thenReturn(firulais.name());
            when(animalEntity.getCode()).thenReturn(firulais.code());
            when(animalJpaRepository.findByIdAndCompany_Id(firulais.id(), EMPRESA))
                    .thenReturn(Optional.of(animalEntity));

            Optional<AnimalRef> resultado = port.findByIdAndCompanyId(firulais.id(), EMPRESA);

            assertThat(resultado).contains(firulais);
        }

        @Test
        @DisplayName("el animal de otra empresa no existe para mi")
        void el_animal_de_otra_empresa_no_existe_para_mi() {
            AnimalRef firulais = LaboratoryTestMother.FIRULAIS;
            when(animalJpaRepository.findByIdAndCompany_Id(firulais.id(), OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(firulais.id(), OTRA_EMPRESA)).isEmpty();
        }
    }
}
