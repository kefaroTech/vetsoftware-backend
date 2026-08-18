package com.vetsoftware.app.branch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.testsupport.BranchMother;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCityQueryPort (branch)")
class JpaCityQueryPortTest {

    @Mock
    private CityJpaRepository cityJpaRepository;
    @Mock
    private CityJpaEntity cityEntity;

    @InjectMocks
    private JpaCityQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la ciudad encontrada a su companion VO")
        void mapea_la_ciudad_encontrada_a_su_companion_vo() {
            when(cityJpaRepository.findById(BranchMother.CITY_ID))
                    .thenReturn(Optional.of(cityEntity));
            when(cityEntity.getId()).thenReturn(BranchMother.BOGOTA.id());
            when(cityEntity.getName()).thenReturn(BranchMother.BOGOTA.name());

            Optional<CityRef> found = port.findById(BranchMother.CITY_ID);

            assertThat(found).contains(BranchMother.BOGOTA);
        }

        @Test
        @DisplayName("una ciudad inexistente devuelve vacío")
        void una_ciudad_inexistente_devuelve_vacio() {
            when(cityJpaRepository.findById(BranchMother.CITY_ID)).thenReturn(Optional.empty());

            assertThat(port.findById(BranchMother.CITY_ID)).isEmpty();
        }
    }
}
