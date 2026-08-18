package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.prescription.domain.ConsultationRef;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaConsultationQueryPort (prescription)")
class JpaConsultationQueryPortTest {

    @Mock
    private ConsultationJpaRepository consultationJpaRepository;
    @Mock
    private ConsultationJpaEntity consultationEntity;

    @InjectMocks
    private JpaConsultationQueryPort port;

    @Test
    @DisplayName("findByIdAndCompanyId mapea la entidad encontrada")
    void find_by_id_and_company_id_mapea_la_entidad() {
        when(consultationJpaRepository.findByIdAndCompany_Id(PrescriptionMother.CONSULTATION_ID,
                PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(consultationEntity));
        when(consultationEntity.getId()).thenReturn(PrescriptionMother.CONSULTATION_ID);
        when(consultationEntity.getDate()).thenReturn(PrescriptionMother.FECHA);

        Optional<ConsultationRef> result = port.findByIdAndCompanyId(
                PrescriptionMother.CONSULTATION_ID, PrescriptionMother.COMPANY_ID);

        assertThat(result).contains(
                new ConsultationRef(PrescriptionMother.CONSULTATION_ID, PrescriptionMother.FECHA));
    }

    @Test
    @DisplayName("findByIdAndCompanyId vacio si la consulta es de otra empresa")
    void find_by_id_and_company_id_vacio_si_es_de_otra_empresa() {
        when(consultationJpaRepository.findByIdAndCompany_Id(PrescriptionMother.CONSULTATION_ID,
                999L)).thenReturn(Optional.empty());

        assertThat(port.findByIdAndCompanyId(PrescriptionMother.CONSULTATION_ID, 999L)).isEmpty();
    }
}
