package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentConfirmationEmailSender;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.BranchQueryPort;
import com.vetsoftware.app.appointment.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Agendamiento de una cita: resolucion de las referencias contra el tenant,
 * aviso de solape y correo de confirmacion. La resolucion de sede tiene su
 * propia clase ({@code CreateAppointmentServiceBranchTest}) y no se repite
 * aqui.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAppointmentService — agendamiento de una cita")
class CreateAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long EMPLOYEE = AppointmentMother.EMPLOYEE_ID;

    @Mock
    private AppointmentRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private OwnerQueryPort ownerQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private BranchQueryPort branchQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private AppointmentConfirmationEmailSender confirmationEmailSender;
    @Mock
    private AppointmentMetrics appointmentMetrics;
    @InjectMocks
    private CreateAppointmentService service;

    private static CreateAppointmentCommand contactoLibre(String clientEmail) {
        return new CreateAppointmentCommand(AppointmentMother.INICIO, AppointmentType.GROOMING,
                EMPLOYEE, null, null, "Walk-in", "3001234567", clientEmail, null,
                AppointmentMother.BRANCH_ID, COMPANY);
    }

    private void stubEmpleadoYSede() {
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(branchQueryPort.findActiveByIdAndCompanyId(AppointmentMother.BRANCH_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.PRINCIPAL));
    }

    private void stubGuardadoSinSolapes() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE), any(), any()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("persiste la cita con las referencias resueltas por los puertos")
        void persiste_la_cita_con_las_referencias_resueltas() {
            stubEmpleadoYSede();
            when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
            when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.DUENO));
            when(ownerQueryPort.findEmailByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.empty());
            stubGuardadoSinSolapes();

            service.execute(AppointmentMother.comandoDeCreacion());

            ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
            verify(repository).save(guardada.capture());
            Appointment cita = guardada.getValue();
            assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.INICIO);
            assertThat(cita.getType()).isEqualTo(AppointmentType.CONSULTATION);
            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
            assertThat(cita.getAnimal()).isEqualTo(AppointmentMother.FIRULAIS);
            assertThat(cita.getOwner()).isEqualTo(AppointmentMother.DUENO);
            assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.VETERINARIA);
            assertThat(cita.getBranch()).isEqualTo(AppointmentMother.PRINCIPAL);
            assertThat(cita.getCompany().id()).isEqualTo(COMPANY);
            assertThat(cita.getNotes()).isEqualTo("Control anual");
        }

        @Test
        @DisplayName("agenda una cita de contacto libre sin animal ni propietario")
        void agenda_una_cita_de_contacto_libre() {
            stubEmpleadoYSede();
            stubGuardadoSinSolapes();

            AppointmentDto dto = service.execute(contactoLibre(null));

            assertThat(dto.animal()).isNull();
            assertThat(dto.owner()).isNull();
            assertThat(dto.clientName()).isEqualTo("Walk-in");
            verifyNoInteractions(animalQueryPort, ownerQueryPort);
        }

        @Test
        @DisplayName("registra la transicion inicial en la telemetria por el canal de staff")
        void registra_la_transicion_inicial_en_la_telemetria() {
            stubEmpleadoYSede();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre(null));

            verify(appointmentMetrics).transitioned(AppointmentStatus.REQUESTED, Channel.STAFF);
        }
    }

    @Nested
    @DisplayName("Aviso de solape")
    class AvisoDeSolape {

        @Test
        @DisplayName("devuelve las citas del mismo veterinario a la misma hora como aviso")
        void devuelve_las_citas_solapadas_como_aviso() {
            stubEmpleadoYSede();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE), eq(AppointmentMother.INICIO),
                    any())).thenReturn(List.of(70L, 71L));

            AppointmentDto dto = service.execute(contactoLibre(null));

            assertThat(dto.overlappingAppointmentIds()).containsExactly(70L, 71L);
        }

        @Test
        @DisplayName("el solape no impide agendar: la cita queda guardada igualmente")
        void el_solape_no_impide_agendar() {
            stubEmpleadoYSede();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(repository.findClashingIds(eq(COMPANY), eq(EMPLOYEE), any(), any()))
                    .thenReturn(List.of(70L));

            AppointmentDto dto = service.execute(contactoLibre(null));

            assertThat(dto.status()).isEqualTo(AppointmentStatus.REQUESTED);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("Correo de confirmacion")
    class CorreoDeConfirmacion {

        @Test
        @DisplayName("envia la confirmacion al correo del propietario registrado")
        void envia_la_confirmacion_al_propietario_registrado() {
            stubEmpleadoYSede();
            when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
            when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.DUENO));
            when(ownerQueryPort.findEmailByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.of("ana@example.com"));
            when(companyQueryPort.findNameById(COMPANY)).thenReturn(Optional.of("Clinica Norte"));
            when(branchQueryPort.findAddressById(AppointmentMother.BRANCH_ID))
                    .thenReturn(Optional.of("Calle 1 #2-3"));
            stubGuardadoSinSolapes();

            service.execute(AppointmentMother.comandoDeCreacion());

            ArgumentCaptor<AppointmentConfirmationData> correo = ArgumentCaptor
                    .forClass(AppointmentConfirmationData.class);
            verify(confirmationEmailSender).send(correo.capture());
            AppointmentConfirmationData datos = correo.getValue();
            assertThat(datos.recipientEmail()).isEqualTo("ana@example.com");
            assertThat(datos.recipientName()).isEqualTo("Ana Ruiz");
            assertThat(datos.companyName()).isEqualTo("Clinica Norte");
            assertThat(datos.startAt()).isEqualTo(AppointmentMother.INICIO);
            assertThat(datos.type()).isEqualTo(AppointmentType.CONSULTATION);
            assertThat(datos.vetName()).isEqualTo("Dra. Vet");
            assertThat(datos.petName()).isEqualTo("Firulais");
            assertThat(datos.branchName()).isEqualTo("Principal");
            assertThat(datos.branchAddress()).isEqualTo("Calle 1 #2-3");
            assertThat(datos.notes()).isEqualTo("Control anual");
        }

        @Test
        @DisplayName("no envia nada si el propietario no tiene correo registrado")
        void no_envia_nada_si_el_propietario_no_tiene_correo() {
            stubEmpleadoYSede();
            when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
            when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.DUENO));
            when(ownerQueryPort.findEmailByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.empty());
            stubGuardadoSinSolapes();

            service.execute(AppointmentMother.comandoDeCreacion());

            verifyNoInteractions(confirmationEmailSender, companyQueryPort);
        }

        @Test
        @DisplayName("descarta un correo del propietario en blanco y no envia")
        void descarta_un_correo_del_propietario_en_blanco() {
            stubEmpleadoYSede();
            when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
            when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.DUENO));
            when(ownerQueryPort.findEmailByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.of("   "));
            stubGuardadoSinSolapes();

            service.execute(AppointmentMother.comandoDeCreacion());

            verifyNoInteractions(confirmationEmailSender, companyQueryPort);
        }

        @Test
        @DisplayName("envia la confirmacion al contacto libre y sin nombre de mascota")
        void envia_la_confirmacion_al_contacto_libre() {
            stubEmpleadoYSede();
            when(companyQueryPort.findNameById(COMPANY)).thenReturn(Optional.empty());
            when(branchQueryPort.findAddressById(AppointmentMother.BRANCH_ID))
                    .thenReturn(Optional.empty());
            stubGuardadoSinSolapes();

            service.execute(contactoLibre("walkin@example.com"));

            ArgumentCaptor<AppointmentConfirmationData> correo = ArgumentCaptor
                    .forClass(AppointmentConfirmationData.class);
            verify(confirmationEmailSender).send(correo.capture());
            AppointmentConfirmationData datos = correo.getValue();
            assertThat(datos.recipientEmail()).isEqualTo("walkin@example.com");
            assertThat(datos.recipientName()).isEqualTo("Walk-in");
            assertThat(datos.petName()).isNull();
            assertThat(datos.companyName()).isNull();
            assertThat(datos.branchAddress()).isNull();
        }

        @Test
        @DisplayName("no envia nada a un contacto libre sin correo")
        void no_envia_nada_a_un_contacto_libre_sin_correo() {
            stubEmpleadoYSede();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre(null));

            verifyNoInteractions(confirmationEmailSender, companyQueryPort);
        }
    }

    /**
     * BE-18: el adaptador de correo es {@code @Async}, asi que encolar el envio
     * dentro de la transaccion lo entregaba sin esperar al desenlace. Estos tests
     * abren una sincronizacion de transaccion a mano —sin contexto de Spring— y
     * comprueban que el correo solo sale si la transaccion confirma.
     */
    @Nested
    @DisplayName("Envio diferido al commit")
    class DiferidoAlCommit {

        @BeforeEach
        void abrirLaSincronizacionDeTransaccion() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void cerrarLaSincronizacionDeTransaccion() {
            TransactionSynchronizationManager.clearSynchronization();
        }

        private void confirmarLaTransaccion() {
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        }

        private void revertirLaTransaccion() {
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sincronizacion -> sincronizacion
                            .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        }

        private void stubDatosDelCorreo() {
            when(companyQueryPort.findNameById(COMPANY)).thenReturn(Optional.of("Clinica Norte"));
            when(branchQueryPort.findAddressById(AppointmentMother.BRANCH_ID))
                    .thenReturn(Optional.of("Calle 1 #2-3"));
        }

        @Test
        @DisplayName("resuelve los datos del correo dentro de la transaccion pero no lo envia")
        void resuelve_los_datos_dentro_de_la_transaccion_pero_no_envia() {
            stubEmpleadoYSede();
            stubDatosDelCorreo();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre("walkin@example.com"));

            verify(companyQueryPort).findNameById(COMPANY);
            verify(branchQueryPort).findAddressById(AppointmentMother.BRANCH_ID);
            verifyNoInteractions(confirmationEmailSender);
        }

        @Test
        @DisplayName("envia el correo cuando la transaccion confirma, con los datos ya resueltos")
        void envia_el_correo_cuando_la_transaccion_confirma() {
            stubEmpleadoYSede();
            stubDatosDelCorreo();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre("walkin@example.com"));
            confirmarLaTransaccion();

            ArgumentCaptor<AppointmentConfirmationData> correo = ArgumentCaptor
                    .forClass(AppointmentConfirmationData.class);
            verify(confirmationEmailSender).send(correo.capture());
            AppointmentConfirmationData datos = correo.getValue();
            assertThat(datos.recipientEmail()).isEqualTo("walkin@example.com");
            assertThat(datos.recipientName()).isEqualTo("Walk-in");
            assertThat(datos.companyName()).isEqualTo("Clinica Norte");
            assertThat(datos.branchAddress()).isEqualTo("Calle 1 #2-3");
        }

        @Test
        @DisplayName("un rollback no envia nada: nadie recibe la confirmacion de una cita fantasma")
        void un_rollback_no_envia_nada() {
            stubEmpleadoYSede();
            stubDatosDelCorreo();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre("walkin@example.com"));
            revertirLaTransaccion();

            verifyNoInteractions(confirmationEmailSender);
        }
    }

    @Nested
    @DisplayName("Aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("no escribe si el veterinario no pertenece a la empresa")
        void no_escribe_si_el_veterinario_no_pertenece_a_la_empresa() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeCreacion()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: 4");

            verifyNoInteractions(repository, animalQueryPort, ownerQueryPort, branchQueryPort,
                    companyQueryPort, confirmationEmailSender, appointmentMetrics);
        }

        @Test
        @DisplayName("no escribe si el animal no pertenece a la empresa")
        void no_escribe_si_el_animal_no_pertenece_a_la_empresa() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
            when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeCreacion()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: 100");

            verifyNoInteractions(repository, ownerQueryPort, branchQueryPort, companyQueryPort,
                    confirmationEmailSender, appointmentMetrics);
        }

        @Test
        @DisplayName("no escribe si el propietario no pertenece a la empresa")
        void no_escribe_si_el_propietario_no_pertenece_a_la_empresa() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
            when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
            when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeCreacion()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Owner not found: 3");

            verify(repository, never()).save(any());
            verifyNoInteractions(branchQueryPort, companyQueryPort, confirmationEmailSender,
                    appointmentMetrics);
        }
    }
}
