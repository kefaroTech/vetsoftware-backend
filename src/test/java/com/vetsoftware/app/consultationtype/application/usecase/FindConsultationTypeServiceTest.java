package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindConsultationTypeService")
class FindConsultationTypeServiceTest {

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private FindConsultationTypeService service;

    @Test
    @DisplayName("encuentra y devuelve el dto del tipo")
    void encuentra_y_devuelve_el_dto() {
        when(repository.findById(ConsultationTypeMother.ID))
                .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));

        ConsultationTypeDto dto = service.findById(ConsultationTypeMother.ID);

        assertThat(dto.id()).isEqualTo(ConsultationTypeMother.ID);
        assertThat(dto.name()).isEqualTo(ConsultationTypeMother.NOMBRE);
    }

    @Test
    @DisplayName("un id inexistente lanza ConsultationTypeNotFoundException")
    void un_id_inexistente_lanza_not_found() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ConsultationTypeNotFoundException.class)
                .hasMessageContaining("ConsultationType not found: 99");
    }
}
