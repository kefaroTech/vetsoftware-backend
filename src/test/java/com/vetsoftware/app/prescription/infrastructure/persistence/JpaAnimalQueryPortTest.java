package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.prescription.domain.AnimalRef;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort (prescription)")
class JpaAnimalQueryPortTest {

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @Mock
    private AnimalJpaEntity animalEntity;

    @InjectMocks
    private JpaAnimalQueryPort port;

    @Test
    @DisplayName("findByIdAndCompanyId mapea la entidad encontrada")
    void find_by_id_and_company_id_mapea_la_entidad() {
        when(animalJpaRepository.findByIdAndCompany_Id(PrescriptionMother.ANIMAL_ID,
                PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(animalEntity));
        when(animalEntity.getId()).thenReturn(PrescriptionMother.ANIMAL_ID);
        when(animalEntity.getName()).thenReturn("Firulais");
        when(animalEntity.getCode()).thenReturn("A-001");

        Optional<AnimalRef> result = port.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID,
                PrescriptionMother.COMPANY_ID);

        assertThat(result)
                .contains(new AnimalRef(PrescriptionMother.ANIMAL_ID, "Firulais", "A-001"));
    }

    @Test
    @DisplayName("findByIdAndCompanyId vacio si el animal es de otra empresa")
    void find_by_id_and_company_id_vacio_si_es_de_otra_empresa() {
        when(animalJpaRepository.findByIdAndCompany_Id(PrescriptionMother.ANIMAL_ID, 999L))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(PrescriptionMother.ANIMAL_ID, 999L)).isEmpty();
    }
}
