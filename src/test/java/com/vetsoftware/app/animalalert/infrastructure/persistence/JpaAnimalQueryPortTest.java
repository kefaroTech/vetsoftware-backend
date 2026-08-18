package com.vetsoftware.app.animalalert.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animalalert.domain.AnimalRef;
import com.vetsoftware.app.animalalert.testsupport.AnimalAlertMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort")
class JpaAnimalQueryPortTest {

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @Mock
    private AnimalJpaEntity animalEntity;

    @InjectMocks
    private JpaAnimalQueryPort port;

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea la fila encontrada a AnimalRef")
        void mapea_la_fila_encontrada_a_animal_ref() {
            when(animalJpaRepository.findByIdAndCompany_Id(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(Optional.of(animalEntity));
            when(animalEntity.getId()).thenReturn(AnimalAlertMother.FIRULAIS.id());
            when(animalEntity.getName()).thenReturn(AnimalAlertMother.FIRULAIS.name());
            when(animalEntity.getCode()).thenReturn(AnimalAlertMother.FIRULAIS.code());

            Optional<AnimalRef> found = port.findByIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID);

            assertThat(found).contains(AnimalAlertMother.FIRULAIS);
        }

        @Test
        @DisplayName("un animal de otra empresa no aparece")
        void un_animal_de_otra_empresa_no_aparece() {
            when(animalJpaRepository.findByIdAndCompany_Id(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(AnimalAlertMother.ANIMAL_ID,
                    AnimalAlertMother.COMPANY_ID)).isEmpty();
        }
    }
}
