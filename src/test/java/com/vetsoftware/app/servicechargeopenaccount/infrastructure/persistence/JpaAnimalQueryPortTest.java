package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.ANIMAL;
import static com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother.COMPANY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort (servicechargeopenaccount)")
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
        @DisplayName("mapea el animal encontrado a su companion VO")
        void mapea_el_animal_encontrado_a_su_companion_vo() {
            when(animalJpaRepository.findByIdAndCompany_Id(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.of(animalEntity));
            when(animalEntity.getId()).thenReturn(ANIMAL.id());
            when(animalEntity.getName()).thenReturn(ANIMAL.name());
            when(animalEntity.getCode()).thenReturn(ANIMAL.code());

            Optional<AnimalRef> found = port.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID);

            assertThat(found).contains(ANIMAL);
        }

        @Test
        @DisplayName("un animal de otra empresa no aparece")
        void un_animal_de_otra_empresa_no_aparece() {
            when(animalJpaRepository.findByIdAndCompany_Id(ANIMAL.id(), COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL.id(), COMPANY_ID)).isEmpty();
        }
    }
}
