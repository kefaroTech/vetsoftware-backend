package com.vetsoftware.app.consultationtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateConsultationTypeService")
class ReactivateConsultationTypeServiceTest {

    @Mock
    private ConsultationTypeRepository repository;

    @InjectMocks
    private ReactivateConsultationTypeService service;

    @Test
    @DisplayName("reactiva y devuelve el tipo ya habilitado")
    void reactiva_y_devuelve_el_tipo_ya_habilitado() {
        when(repository.reactivate(ConsultationTypeMother.ID)).thenReturn(1);
        when(repository.findById(ConsultationTypeMother.ID))
                .thenReturn(Optional.of(ConsultationTypeMother.consultaGeneral()));

        ConsultationTypeDto dto = service.execute(ConsultationTypeMother.ID);

        assertThat(dto.id()).isEqualTo(ConsultationTypeMother.ID);
        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(ConsultationTypeMother.ID);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        when(repository.reactivate(99L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(99L))
                .isInstanceOf(ConsultationTypeNotFoundException.class)
                .hasMessageContaining("ConsultationType not found: 99");

        verify(repository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("si el tipo desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
    void si_el_tipo_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(ConsultationTypeMother.ID)).thenReturn(1);
        when(repository.findById(ConsultationTypeMother.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ConsultationTypeMother.ID))
                .isInstanceOf(ConsultationTypeNotFoundException.class);
    }
}
