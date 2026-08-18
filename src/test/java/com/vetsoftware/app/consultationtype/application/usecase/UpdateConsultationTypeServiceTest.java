package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateConsultationTypeService")
class UpdateConsultationTypeServiceTest {

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private UpdateConsultationTypeService service;

    @Test
    @DisplayName("actualiza nombre y descripcion sobre el tipo existente y lo persiste")
    void actualiza_nombre_y_descripcion_y_persiste() {
        when(repository.findById(ConsultationTypeMother.ID))
                .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationTypeDto dto = service.execute(new UpdateConsultationTypeCommand(
                ConsultationTypeMother.ID, "Nuevo nombre", "Nueva descripcion de la consulta"));

        assertThat(dto.name()).isEqualTo("Nuevo nombre");
        assertThat(dto.description()).isEqualTo("Nueva descripcion de la consulta");
        ArgumentCaptor<ConsultationType> captor = ArgumentCaptor.forClass(ConsultationType.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(ConsultationTypeMother.ID);
    }

    @Test
    @DisplayName("un id inexistente lanza y no guarda")
    void un_id_inexistente_lanza_y_no_guarda() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new UpdateConsultationTypeCommand(99L, "Nombre",
                "Descripcion valida de la consulta")))
                .isInstanceOf(ConsultationTypeNotFoundException.class)
                .hasMessageContaining("ConsultationType not found: 99");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("un nombre invalido lanza y no guarda")
    void un_nombre_invalido_lanza_y_no_guarda() {
        when(repository.findById(ConsultationTypeMother.ID))
                .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));

        assertThatThrownBy(
                () -> service.execute(new UpdateConsultationTypeCommand(ConsultationTypeMother.ID,
                        "", "Descripcion valida de la consulta")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        verify(repository, never()).save(any());
    }
}
