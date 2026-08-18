package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateConsultationTypeService")
class CreateConsultationTypeServiceTest {

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private CreateConsultationTypeService service;

    @Test
    @DisplayName("crea el tipo con los datos del comando y devuelve su dto")
    void crea_el_tipo_con_los_datos_del_comando() {
        when(repository.save(any())).thenReturn(ConsultationTypeMother.consultaGeneral());

        ConsultationTypeDto dto = service
                .execute(new CreateConsultationTypeCommand("Vacunacion", "Aplicacion de vacunas"));

        assertThat(dto.id()).isEqualTo(ConsultationTypeMother.ID);
        ArgumentCaptor<ConsultationType> captor = ArgumentCaptor.forClass(ConsultationType.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Vacunacion");
        assertThat(captor.getValue().getDescription()).isEqualTo("Aplicacion de vacunas");
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("un nombre invalido lanza y no toca el repositorio")
    void nombre_invalido_lanza_y_no_toca_el_repositorio() {
        CreateConsultationTypeCommand command = new CreateConsultationTypeCommand("",
                "Aplicacion de vacunas");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("una descripcion invalida lanza y no toca el repositorio")
    void descripcion_invalida_lanza_y_no_toca_el_repositorio() {
        CreateConsultationTypeCommand command = new CreateConsultationTypeCommand("Vacunacion", "");

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description is required");

        verifyNoInteractions(repository);
    }
}
