package com.vetsoftware.app.consultation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.domain.ConsultationNotFoundException;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindConsultationService")
class FindConsultationServiceTest {

    @Mock
    private ConsultationRepository repository;

    @InjectMocks
    private FindConsultationService service;

    @Test
    @DisplayName("devuelve el DTO de la consulta encontrada, acotada por empresa")
    void devuelve_el_dto_de_la_consulta_encontrada() {
        when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID))
                .thenReturn(Optional.of(ConsultationMother.consultaValida()));

        ConsultationDto dto = service.findById(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ConsultationMother.CONSULTATION_ID);
        assertThat(dto.anamnesis()).isEqualTo("Anamnesis del paciente");
    }

    @Test
    @DisplayName("una consulta inexistente en la empresa lanza ConsultationNotFoundException")
    void una_consulta_inexistente_lanza_not_found() {
        when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).isInstanceOf(ConsultationNotFoundException.class)
                .hasMessageContaining(
                        "Consultation not found: " + ConsultationMother.CONSULTATION_ID);
    }
}
