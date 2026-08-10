package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateAppointmentService — edicion de una cita existente")
class UpdateAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;
    private static final Long EMPLOYEE = AppointmentMother.EMPLOYEE_ID;

    @Mock
    private AppointmentRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private OwnerQueryPort ownerQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @InjectMocks
    private UpdateAppointmentService service;

    private void stubCitaExistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
    }

    private void stubReferenciasCompletas() {
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
        when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.DUENO));
    }

    @Test
    @DisplayName("guarda la cita con la hora, el tipo y el veterinario nuevos")
    void guarda_la_cita_con_los_datos_nuevos() {
        stubCitaExistente();
        stubReferenciasCompletas();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE), any(), any()))
                .thenReturn(List.of());

        service.execute(AppointmentMother.comandoDeActualizacion());

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        Appointment cita = guardada.getValue();
        assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.NUEVO_INICIO);
        assertThat(cita.getType()).isEqualTo(AppointmentType.SURGERY);
        assertThat(cita.getNotes()).isEqualTo("Reprogramada");
        assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.OTRO_VETERINARIO);
        assertThat(cita.getAnimal()).isEqualTo(AppointmentMother.FIRULAIS);
        assertThat(cita.getOwner()).isEqualTo(AppointmentMother.DUENO);
    }

    @Test
    @DisplayName("conserva la empresa, la sede y el estado de la cita original")
    void conserva_la_empresa_la_sede_y_el_estado() {
        stubCitaExistente();
        stubReferenciasCompletas();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE), any(), any()))
                .thenReturn(List.of());

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeActualizacion());

        assertThat(dto.branch().id()).isEqualTo(AppointmentMother.BRANCH_ID);
        assertThat(dto.status()).isEqualTo(AppointmentStatus.REQUESTED);
    }

    @Test
    @DisplayName("devuelve el aviso de solape recalculado con la hora nueva")
    void devuelve_el_aviso_de_solape_recalculado() {
        stubCitaExistente();
        stubReferenciasCompletas();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE),
                eq(AppointmentMother.NUEVO_INICIO), eq(ID))).thenReturn(List.of(81L));

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeActualizacion());

        assertThat(dto.overlappingAppointmentIds()).containsExactly(81L);
    }

    @Test
    @DisplayName("convierte la cita en contacto libre cuando se quitan animal y propietario")
    void convierte_la_cita_en_contacto_libre() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE), any(), any()))
                .thenReturn(List.of());

        AppointmentDto dto = service.execute(new UpdateAppointmentCommand(ID,
                AppointmentMother.NUEVO_INICIO, AppointmentType.GROOMING, EMPLOYEE, null, null,
                "Walk-in", "3001234567", "walkin@example.com", null, COMPANY));

        assertThat(dto.animal()).isNull();
        assertThat(dto.owner()).isNull();
        assertThat(dto.clientName()).isEqualTo("Walk-in");
        verifyNoInteractions(animalQueryPort, ownerQueryPort);
    }

    @Test
    @DisplayName("una cita de otra empresa es inexistente y no se escribe nada")
    void una_cita_de_otra_empresa_es_inexistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeActualizacion()))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");

        verify(repository, never()).save(any());
        verifyNoInteractions(employeeQueryPort, animalQueryPort, ownerQueryPort);
    }

    @Test
    @DisplayName("no escribe si el veterinario no pertenece a la empresa")
    void no_escribe_si_el_veterinario_no_pertenece_a_la_empresa() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeActualizacion()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee not found: 4");

        verify(repository, never()).save(any());
        verifyNoInteractions(animalQueryPort, ownerQueryPort);
    }

    @Test
    @DisplayName("no escribe si el animal no pertenece a la empresa")
    void no_escribe_si_el_animal_no_pertenece_a_la_empresa() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeActualizacion()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Animal not found: 100");

        verify(repository, never()).save(any());
        verifyNoInteractions(ownerQueryPort);
    }

    @Test
    @DisplayName("no escribe si el propietario no pertenece a la empresa")
    void no_escribe_si_el_propietario_no_pertenece_a_la_empresa() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
        when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeActualizacion()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner not found: 3");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("propaga la invariante del dominio y no guarda una cita sin sujeto")
    void propaga_la_invariante_del_dominio_y_no_guarda() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));

        assertThatThrownBy(() -> service.execute(new UpdateAppointmentCommand(ID,
                AppointmentMother.NUEVO_INICIO, AppointmentType.GROOMING, EMPLOYEE, null, null,
                null, null, null, null, COMPANY))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one of");

        verify(repository, never()).save(any());
    }
}
