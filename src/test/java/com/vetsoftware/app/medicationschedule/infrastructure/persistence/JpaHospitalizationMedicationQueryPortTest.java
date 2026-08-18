package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaRepository;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Adaptador de lectura de la orden de medicacion que el generador de dominio
 * necesita. Los enums de la orden (frequency, guidelineType, ...) llegan como
 * String plano — vertical slicing, este mapper no conoce el dominio de
 * hospitalizationmedication.
 *
 * <p>
 * La variante acotada por empresa es ademas la que autoriza escribir sobre una
 * toma: la toma no tiene {@code company_id}, asi que su propiedad se prueba
 * subiendo a la orden y de ahi a la hospitalizacion.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaHospitalizationMedicationQueryPort")
class JpaHospitalizationMedicationQueryPortTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private HospitalizationMedicationJpaRepository medicationJpaRepository;
    @InjectMocks
    private JpaHospitalizationMedicationQueryPort port;

    private static HospitalizationMedicationJpaEntity ordenDeAmoxicilina() {
        HospitalizationJpaEntity hospitalization = mock(HospitalizationJpaEntity.class);
        when(hospitalization.getId()).thenReturn(400L);

        HospitalizationMedicationJpaEntity entity = mock(HospitalizationMedicationJpaEntity.class);
        when(entity.getId()).thenReturn(300L);
        when(entity.getName()).thenReturn("Amoxicilina 500mg");
        when(entity.getHospitalization()).thenReturn(hospitalization);
        when(entity.getFrequency()).thenReturn("EVERY_8H");
        when(entity.getGuidelineType()).thenReturn("FIXED");
        when(entity.getDurationMeasure()).thenReturn("DAYS");
        when(entity.getDurationQuantity()).thenReturn(3);
        when(entity.getStartDate()).thenReturn(LocalDate.of(2026, 1, 15));
        when(entity.getStartTime()).thenReturn(LocalTime.of(8, 0));
        return entity;
    }

    private static MedicationOrderParams parametrosEsperados() {
        return new MedicationOrderParams(300L, "Amoxicilina 500mg", 400L, "EVERY_8H", "FIXED",
                "DAYS", 3, LocalDate.of(2026, 1, 15), LocalTime.of(8, 0));
    }

    @Test
    @DisplayName("mapea la orden encontrada a sus parametros de generacion")
    void mapea_la_orden_encontrada_a_sus_parametros() {
        // La entidad se construye ANTES del when: sus propios stubs dentro de un
        // thenReturn dejarian a medias el stubbing del repositorio.
        Optional<HospitalizationMedicationJpaEntity> orden = Optional.of(ordenDeAmoxicilina());
        when(medicationJpaRepository.findById(300L)).thenReturn(orden);

        Optional<MedicationOrderParams> params = port.findById(300L);

        assertThat(params).contains(parametrosEsperados());
    }

    @Test
    @DisplayName("devuelve vacio si la orden no existe")
    void devuelve_vacio_si_la_orden_no_existe() {
        when(medicationJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(port.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("acotado por empresa mapea la orden del tenant")
    void acotado_por_empresa_mapea_la_orden_del_tenant() {
        Optional<HospitalizationMedicationJpaEntity> orden = Optional.of(ordenDeAmoxicilina());
        when(medicationJpaRepository.findByIdAndHospitalization_Company_Id(300L, COMPANY_ID))
                .thenReturn(orden);

        assertThat(port.findByIdAndCompanyId(300L, COMPANY_ID)).contains(parametrosEsperados());
    }

    @Test
    @DisplayName("la orden de otra empresa no resuelve, aunque el id exista")
    void la_orden_de_otra_empresa_no_resuelve() {
        when(medicationJpaRepository.findByIdAndHospitalization_Company_Id(300L, OTRA_EMPRESA))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(300L, OTRA_EMPRESA)).isEmpty();
    }
}
