package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaHospitalizationQueryPort")
class JpaHospitalizationQueryPortTest {

    @Mock
    private HospitalizationJpaRepository hospitalizationJpaRepository;
    @Mock
    private HospitalizationJpaEntity entity;

    @InjectMocks
    private JpaHospitalizationQueryPort port;

    @Test
    @DisplayName("hospitalizacion encontrada se mapea a HospitalizationRef")
    void hospitalizacion_encontrada_se_mapea_a_hospitalization_ref() {
        LocalDate fecha = LocalDate.of(2026, 3, 1);
        when(hospitalizationJpaRepository.findByIdAndCompany_Id(600L, 9L))
                .thenReturn(Optional.of(entity));
        when(entity.getId()).thenReturn(600L);
        when(entity.getDate()).thenReturn(fecha);

        Optional<HospitalizationRef> resultado = port.findByIdAndCompanyId(600L, 9L);

        assertThat(resultado).contains(new HospitalizationRef(600L, fecha));
    }

    @Test
    @DisplayName("hospitalizacion inexistente devuelve vacio")
    void hospitalizacion_inexistente_devuelve_vacio() {
        when(hospitalizationJpaRepository.findByIdAndCompany_Id(99L, 9L))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(99L, 9L)).isEmpty();
    }
}
