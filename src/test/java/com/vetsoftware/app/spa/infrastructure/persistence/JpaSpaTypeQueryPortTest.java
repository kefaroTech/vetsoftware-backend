package com.vetsoftware.app.spa.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.domain.SpaTypeRef;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaEntity;
import com.vetsoftware.app.spatype.infrastructure.persistence.SpaTypeJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSpaTypeQueryPort (spa)")
class JpaSpaTypeQueryPortTest {

    @Mock
    private SpaTypeJpaRepository spaTypeJpaRepository;
    @Mock
    private SpaTypeJpaEntity spaTypeEntity;

    @InjectMocks
    private JpaSpaTypeQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("un tipo de spa existente se mapea a su companion VO")
        void tipo_existente_se_mapea_a_spa_type_ref() {
            when(spaTypeJpaRepository.findById(20L)).thenReturn(Optional.of(spaTypeEntity));
            when(spaTypeEntity.getId()).thenReturn(20L);
            when(spaTypeEntity.getName()).thenReturn("Baño básico");

            Optional<SpaTypeRef> encontrado = port.findById(20L);

            assertThat(encontrado).contains(new SpaTypeRef(20L, "Baño básico"));
        }

        @Test
        @DisplayName("un tipo de spa inexistente devuelve vacio")
        void tipo_inexistente_devuelve_vacio() {
            when(spaTypeJpaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(port.findById(99L)).isEmpty();
        }
    }
}
