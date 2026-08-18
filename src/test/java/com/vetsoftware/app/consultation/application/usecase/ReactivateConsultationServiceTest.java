package com.vetsoftware.app.consultation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateConsultationService")
class ReactivateConsultationServiceTest {

    @Mock
    private ConsultationRepository repository;

    @InjectMocks
    private ReactivateConsultationService service;

    @Test
    @DisplayName("reactiva y devuelve la consulta ya habilitada")
    void reactiva_y_devuelve_la_consulta_ya_habilitada() {
        when(repository.reactivate(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID))
                .thenReturn(Optional.of(ConsultationMother.consultaValida()));

        ConsultationDto dto = service.execute(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(ConsultationMother.CONSULTATION_ID);
        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        // El UPDATE ya lleva el companyId, asi que 0 filas cubre a la vez "no existe" y
        // "es de otra empresa". No hace falta un SELECT previo.
        when(repository.reactivate(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).isInstanceOf(ConsultationNotFoundException.class)
                .hasMessageContaining(
                        "Consultation not found: " + ConsultationMother.CONSULTATION_ID);

        verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("si la consulta desaparece entre el UPDATE y el SELECT, falla como no-encontrada")
    void si_la_consulta_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ConsultationMother.CONSULTATION_ID,
                ConsultationMother.COMPANY_ID)).isInstanceOf(ConsultationNotFoundException.class);
    }
}
