package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAppointmentService — baja logica de una cita")
class DeleteAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;

    @Mock
    private AppointmentRepository repository;
    @InjectMocks
    private DeleteAppointmentService service;

    @Test
    @DisplayName("borra la cita acotando el borrado a la empresa del solicitante")
    void borra_la_cita_acotando_a_la_empresa() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));

        assertThatCode(() -> service.execute(ID, COMPANY)).doesNotThrowAnyException();

        verify(repository).delete(ID, COMPANY);
    }

    @Test
    @DisplayName("una cita de otra empresa es inexistente y no se borra nada")
    void una_cita_de_otra_empresa_es_inexistente() {
        when(repository.findByIdAndCompanyId(ID, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(ID, 999L))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");

        verify(repository, never()).delete(any(), any());
    }
}
