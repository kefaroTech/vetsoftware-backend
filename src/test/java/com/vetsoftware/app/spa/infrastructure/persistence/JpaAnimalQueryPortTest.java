package com.vetsoftware.app.spa.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.spa.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort (spa)")
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
        @DisplayName("un animal de la empresa se mapea a su companion VO")
        void un_animal_de_la_empresa_se_mapea_a_su_companion_vo() {
            when(animalJpaRepository.findByIdAndCompany_Id(1L, 10L))
                    .thenReturn(Optional.of(animalEntity));
            when(animalEntity.getId()).thenReturn(1L);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");

            Optional<AnimalRef> encontrado = port.findByIdAndCompanyId(1L, 10L);

            assertThat(encontrado).contains(new AnimalRef(1L, "Firulais", "A-001"));
        }

        @Test
        @DisplayName("un animal de otra empresa devuelve vacio")
        void un_animal_de_otra_empresa_devuelve_vacio() {
            when(animalJpaRepository.findByIdAndCompany_Id(1L, 99L)).thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(1L, 99L)).isEmpty();
        }
    }
}
