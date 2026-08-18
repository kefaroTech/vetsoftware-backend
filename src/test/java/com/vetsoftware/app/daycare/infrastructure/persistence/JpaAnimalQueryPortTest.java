package com.vetsoftware.app.daycare.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalQueryPort (daycare)")
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
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("un animal de mi empresa se mapea a su companion VO")
        void mapea_el_animal_encontrado_a_su_companion_vo() {
            when(animalJpaRepository.findByIdAndCompany_Id(1L, EMPRESA))
                    .thenReturn(Optional.of(animalEntity));
            when(animalEntity.getId()).thenReturn(1L);
            when(animalEntity.getName()).thenReturn("Firulais");
            when(animalEntity.getCode()).thenReturn("A-001");

            Optional<AnimalRef> encontrado = port.findByIdAndCompanyId(1L, EMPRESA);

            assertThat(encontrado).contains(new AnimalRef(1L, "Firulais", "A-001"));
        }

        @Test
        @DisplayName("el animal de otra empresa no existe para mi")
        void el_animal_de_otra_empresa_no_existe_para_mi() {
            when(animalJpaRepository.findByIdAndCompany_Id(1L, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(1L, OTRA_EMPRESA)).isEmpty();
        }
    }
}
