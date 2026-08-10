package com.vetsoftware.app.appointment.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppointmentDto — proyeccion campo por campo del agregado")
class AppointmentDtoTest {

    @Test
    @DisplayName("copia cada campo de la cita, incluidos animal y propietario")
    void copia_cada_campo_de_la_cita() {
        Appointment cita = AppointmentMother.conEstado(AppointmentStatus.CONFIRMED);

        AppointmentDto dto = AppointmentDto.from(cita);

        assertThat(dto.id()).isEqualTo(AppointmentMother.APPOINTMENT_ID);
        assertThat(dto.startAt()).isEqualTo(AppointmentMother.INICIO);
        assertThat(dto.type()).isEqualTo(AppointmentType.CONSULTATION);
        assertThat(dto.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(dto.notes()).isEqualTo("Control anual");
        assertThat(dto.cancellationReason()).isNull();
        assertThat(dto.animal())
                .isEqualTo(new AnimalSummaryDto(AppointmentMother.ANIMAL_ID, "Firulais", "A-001"));
        assertThat(dto.owner())
                .isEqualTo(new OwnerSummaryDto(AppointmentMother.OWNER_ID, "Ana Ruiz"));
        assertThat(dto.clientName()).isNull();
        assertThat(dto.clientPhone()).isNull();
        assertThat(dto.clientEmail()).isNull();
        assertThat(dto.employee())
                .isEqualTo(new EmployeeSummaryDto(AppointmentMother.EMPLOYEE_ID, "Dra. Vet"));
        assertThat(dto.branch()).isEqualTo(
                new BranchSummaryDto(AppointmentMother.BRANCH_ID, "Principal", "PRINCIPAL"));
        assertThat(dto.version()).isEqualTo(3L);
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.createdDate()).isEqualTo(AppointmentMother.CREADA);
    }

    @Test
    @DisplayName("deja en null los resumenes de animal y propietario en una cita de contacto libre")
    void deja_en_null_los_resumenes_de_contacto_libre() {
        AppointmentDto dto = AppointmentDto
                .from(AppointmentMother.deContactoLibre("walkin@example.com"));

        assertThat(dto.animal()).isNull();
        assertThat(dto.owner()).isNull();
        assertThat(dto.clientName()).isEqualTo("Walk-in");
        assertThat(dto.clientPhone()).isEqualTo("3001234567");
        assertThat(dto.clientEmail()).isEqualTo("walkin@example.com");
    }

    @Test
    @DisplayName("sin solapes explicitos la lista de citas en conflicto va vacia, nunca null")
    void sin_solapes_explicitos_la_lista_va_vacia() {
        AppointmentDto dto = AppointmentDto.from(AppointmentMother.solicitada());

        assertThat(dto.overlappingAppointmentIds()).isEmpty();
    }

    @Test
    @DisplayName("traslada los identificadores de las citas que se solapan")
    void traslada_los_identificadores_de_las_citas_que_se_solapan() {
        AppointmentDto dto = AppointmentDto.from(AppointmentMother.solicitada(), List.of(70L, 71L));

        assertThat(dto.overlappingAppointmentIds()).containsExactly(70L, 71L);
    }

    @Test
    @DisplayName("una lista de solapes null se normaliza a lista vacia")
    void una_lista_de_solapes_null_se_normaliza_a_vacia() {
        AppointmentDto dto = AppointmentDto.from(AppointmentMother.solicitada(), null);

        assertThat(dto.overlappingAppointmentIds()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("expone el motivo de cancelacion de una cita cancelada")
    void expone_el_motivo_de_cancelacion() {
        AppointmentDto dto = AppointmentDto.from(AppointmentMother.cancelada("El dueno aviso"));

        assertThat(dto.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(dto.cancellationReason()).isEqualTo("El dueno aviso");
        assertThat(dto.enabled()).isFalse();
    }
}
