package com.vetsoftware.app.deworming.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaConsultationQueryPort — deworming")
class JpaConsultationQueryPortTest {

    @Mock
    private ConsultationJpaRepository consultationJpaRepository;
    @InjectMocks
    private JpaConsultationQueryPort port;

    private static ConsultationJpaEntity consultaEncontrada(long id, LocalDate date) {
        ConsultationJpaEntity entity = mock(ConsultationJpaEntity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getDate()).thenReturn(date);
        return entity;
    }

    @Test
    @DisplayName("mapea la consulta de la empresa a su companion VO")
    void mapea_la_consulta_encontrada_a_su_companion_vo() {
        LocalDate fecha = LocalDate.of(2026, 3, 1);
        ConsultationJpaEntity consulta = consultaEncontrada(200L, fecha);
        when(consultationJpaRepository.findByIdAndCompany_Id(200L, 9L))
                .thenReturn(Optional.of(consulta));

        Optional<ConsultationRef> ref = port.findByIdAndCompanyId(200L, 9L);

        assertThat(ref).contains(new ConsultationRef(200L, fecha));
    }

    @Test
    @DisplayName("devuelve vacio si la consulta no existe")
    void devuelve_vacio_si_la_consulta_no_existe() {
        when(consultationJpaRepository.findByIdAndCompany_Id(999L, 9L))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(999L, 9L)).isEmpty();
    }

    @Test
    @DisplayName("devuelve vacio si la consulta es de otra empresa")
    void devuelve_vacio_si_la_consulta_es_de_otra_empresa() {
        when(consultationJpaRepository.findByIdAndCompany_Id(200L, 77L))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(200L, 77L)).isEmpty();
    }
}
