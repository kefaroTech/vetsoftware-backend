package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort (appointment) — adaptador sobre AnimalJpaRepository")
class JpaAnimalQueryPortTest {

    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @InjectMocks
    private JpaAnimalQueryPort port;

    @Test
    @DisplayName("mapea el animal encontrado en la empresa al AnimalRef")
    void mapea_el_animal_encontrado_en_la_empresa() {
        AnimalJpaEntity entity = mock(AnimalJpaEntity.class);
        when(entity.getId()).thenReturn(ANIMAL_ID);
        when(entity.getName()).thenReturn("Firulais");
        when(entity.getCode()).thenReturn("A-001");
        when(animalJpaRepository.findByIdAndCompany_Id(ANIMAL_ID, COMPANY_ID))
                .thenReturn(Optional.of(entity));

        assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID))
                .contains(new AnimalRef(ANIMAL_ID, "Firulais", "A-001"));
    }

    @Test
    @DisplayName("un animal inexistente o de otra empresa no se entrega")
    void un_animal_inexistente_o_de_otra_empresa_no_se_entrega() {
        when(animalJpaRepository.findByIdAndCompany_Id(ANIMAL_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID)).isEmpty();
    }
}
