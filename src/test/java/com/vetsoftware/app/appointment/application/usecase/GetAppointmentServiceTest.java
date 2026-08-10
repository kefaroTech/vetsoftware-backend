package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAppointmentService — consulta de una cita por id")
class GetAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;

    @Mock
    private AppointmentRepository repository;
    @InjectMocks
    private GetAppointmentService service;

    @Test
    @DisplayName("devuelve la cita de la empresa proyectada como DTO")
    void devuelve_la_cita_proyectada_como_dto() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(AppointmentStatus.ARRIVED)));

        AppointmentDto dto = service.findById(ID, COMPANY);

        assertThat(dto.id()).isEqualTo(ID);
        assertThat(dto.status()).isEqualTo(AppointmentStatus.ARRIVED);
        assertThat(dto.employee().name()).isEqualTo("Dra. Vet");
        assertThat(dto.branch().code()).isEqualTo("PRINCIPAL");
    }

    @Test
    @DisplayName("una consulta puntual no trae avisos de solape")
    void una_consulta_puntual_no_trae_avisos_de_solape() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));

        assertThat(service.findById(ID, COMPANY).overlappingAppointmentIds()).isEmpty();
    }

    @Test
    @DisplayName("una cita de otra empresa se reporta como inexistente")
    void una_cita_de_otra_empresa_se_reporta_como_inexistente() {
        when(repository.findByIdAndCompanyId(ID, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID, 999L))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");
    }
}
