package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaConsultationQueryPort — adaptador sobre ConsultationJpaRepository")
class JpaConsultationQueryPortTest {

    private static final long EMPRESA = 77L;
    private static final long OTRA_EMPRESA = 88L;

    @Mock
    private ConsultationJpaRepository consultationJpaRepository;
    @Mock
    private ConsultationJpaEntity consultationEntity;
    @InjectMocks
    private JpaConsultationQueryPort port;

    @Nested
    @DisplayName("busqueda acotada por empresa")
    class Busqueda {

        @Test
        @DisplayName("mapea la consulta de mi empresa a su companion VO")
        void mapea_la_consulta_encontrada_a_su_companion_vo() {
            ConsultationRef consulta = LaboratoryTestMother.CONSULTA;
            when(consultationEntity.getId()).thenReturn(consulta.id());
            when(consultationEntity.getDate()).thenReturn(consulta.date());
            when(consultationJpaRepository.findByIdAndCompany_Id(consulta.id(), EMPRESA))
                    .thenReturn(Optional.of(consultationEntity));

            Optional<ConsultationRef> resultado = port.findByIdAndCompanyId(consulta.id(), EMPRESA);

            assertThat(resultado).contains(consulta);
        }

        @Test
        @DisplayName("la consulta de otra empresa no existe para mi")
        void la_consulta_de_otra_empresa_no_existe_para_mi() {
            ConsultationRef consulta = LaboratoryTestMother.CONSULTA;
            when(consultationJpaRepository.findByIdAndCompany_Id(consulta.id(), OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(consulta.id(), OTRA_EMPRESA)).isEmpty();
        }
    }
}
