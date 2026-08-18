package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Consulta SIEMPRE el finder acotado por empresa: el puerto no ofrece variante
 * ancha, para que la fila propia no pueda colgarse de la consulta de otro
 * tenant.
 */
@ExtendWith(MockitoExtension.class)
class JpaConsultationQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

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
    @DisplayName("mapea la consulta de mi empresa a su companion VO")
    void mapea_la_consulta_encontrada_a_su_companion_vo() {
        LocalDate fecha = LocalDate.of(2026, 2, 28);
        ConsultationJpaEntity consulta = consultaEncontrada(7L, fecha);
        when(consultationJpaRepository.findByIdAndCompany_Id(7L, EMPRESA))
                .thenReturn(Optional.of(consulta));

        Optional<ConsultationRef> ref = port.findByIdAndCompanyId(7L, EMPRESA);

        assertThat(ref).contains(new ConsultationRef(7L, fecha));
    }

    @Test
    @DisplayName("la consulta de otra empresa no existe para mi")
    void la_consulta_de_otra_empresa_no_existe_para_mi() {
        when(consultationJpaRepository.findByIdAndCompany_Id(7L, OTRA_EMPRESA))
                .thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(7L, OTRA_EMPRESA)).isEmpty();
    }
}
