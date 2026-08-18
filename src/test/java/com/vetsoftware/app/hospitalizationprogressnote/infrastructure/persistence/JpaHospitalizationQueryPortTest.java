package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de resolucion de hospitalizacion para las notas de evolucion. Sin
 * filtro de empresa: el companion VO solo trae la fecha para mostrar, y el
 * scope de empresa lo aplica el repositorio de la propia nota
 * ({@code findByIdAndHospitalization_Company_Id}).
 */
@ExtendWith(MockitoExtension.class)
class JpaHospitalizationQueryPortTest {

    @Mock
    private HospitalizationJpaRepository hospitalizationJpaRepository;
    @InjectMocks
    private JpaHospitalizationQueryPort port;

    @Test
    @DisplayName("mapea la hospitalizacion encontrada a su companion VO")
    void mapea_la_hospitalizacion_encontrada_a_su_companion_vo() {
        HospitalizationJpaEntity entity = mock(HospitalizationJpaEntity.class);
        when(entity.getId()).thenReturn(55L);
        when(entity.getDate()).thenReturn(LocalDate.of(2026, 3, 1));
        when(hospitalizationJpaRepository.findById(55L)).thenReturn(Optional.of(entity));

        Optional<HospitalizationRef> ref = port.findById(55L);

        assertThat(ref).contains(new HospitalizationRef(55L, LocalDate.of(2026, 3, 1)));
    }

    @Test
    @DisplayName("devuelve vacio si la hospitalizacion no existe")
    void devuelve_vacio_si_la_hospitalizacion_no_existe() {
        when(hospitalizationJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }
}
