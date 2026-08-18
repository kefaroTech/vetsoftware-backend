package com.vetsoftware.app.problem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.problem.domain.AnimalRef;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort (problem)")
class JpaAnimalQueryPortTest {

    @Mock
    private AnimalJpaRepository animalJpaRepository;

    @InjectMocks
    private JpaAnimalQueryPort port;

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea el animal encontrado a su AnimalRef")
        void mapea_el_animal_encontrado() {
            AnimalJpaEntity entidad = mock(AnimalJpaEntity.class);
            when(entidad.getId()).thenReturn(ProblemMother.ANIMAL_ID);
            when(entidad.getName()).thenReturn("Firulais");
            when(entidad.getCode()).thenReturn("A-001");
            when(animalJpaRepository.findByIdAndCompany_Id(ProblemMother.ANIMAL_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.of(entidad));

            Optional<AnimalRef> ref = port.findByIdAndCompanyId(ProblemMother.ANIMAL_ID,
                    ProblemMother.COMPANY_ID);

            assertThat(ref).contains(new AnimalRef(ProblemMother.ANIMAL_ID, "Firulais", "A-001"));
        }

        @Test
        @DisplayName("devuelve vacio si el animal no existe o es de otra empresa")
        void devuelve_vacio_si_no_existe() {
            when(animalJpaRepository.findByIdAndCompany_Id(ProblemMother.ANIMAL_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ProblemMother.ANIMAL_ID, ProblemMother.COMPANY_ID))
                    .isEmpty();
        }
    }
}
