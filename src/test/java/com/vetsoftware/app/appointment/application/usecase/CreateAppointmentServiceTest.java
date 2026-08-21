package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentConfirmationEmailSender;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.BranchQueryPort;
import com.vetsoftware.app.appointment.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
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
import org.mockito.InOrder;
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
    @Mock
    private AppointmentDurationPolicyPort durationPolicyPort;
    @InjectMocks
    private CreateAppointmentService service;

    private static final int DEFECTO = AppointmentMother.DURACION_POR_DEFECTO;

    private static CreateAppointmentCommand contactoLibre(String clientEmail) {
        return contactoLibre(clientEmail, false);
    }

    private static CreateAppointmentCommand contactoLibre(String clientEmail,
            boolean forceOverlap) {
        return new CreateAppointmentCommand(AppointmentMother.INICIO, null,
                AppointmentType.GROOMING, EMPLOYEE, null, null, "Walk-in", "3001234567",
                clientEmail, null, AppointmentMother.BRANCH_ID, COMPANY, forceOverlap,
                AppointmentMother.SEDES_VISIBLES);
    }

    private void stubEmpleadoYSede() {
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(branchQueryPort.findActiveByIdAndCompanyId(AppointmentMother.BRANCH_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.PRINCIPAL));
    }

    /** La agenda esta libre: ni la politica ni la consulta encuentran nada. */
    private void stubGuardadoSinSolapes() {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                isNull())).thenReturn(List.of());
    }

    /**
     * La agenda del veterinario ya tiene esas citas en el hueco, todas en una sede
     * que el caller ve.
     */
    private void stubSolapeCon(List<Long> ids) {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                isNull()))
                .thenReturn(ids.stream().map(
                        id -> new AppointmentRepository.Overlap(id, AppointmentMother.BRANCH_ID))
                        .toList());
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

    /**
     * Issue #114. La carrera: dos peticiones concurrentes para el mismo veterinario
     * llegan a {@code findOverlapping} antes de que ninguna haya guardado, las dos
     * ven el hueco libre y se agendan encima. El lock pesimista por empleado
     * serializa ese leer-y-escribir, y solo sirve si se toma ANTES de la lectura:
     * detras de ella la ventana vuelve a abrirse entera y ningun otro test del
     * repositorio se entera, porque las llamadas siguen ahi.
     *
     * <p>
     * El indice unico {@code uq_appointments_active_employee_start} (changeset 226)
     * es la otra mitad y solo cubre el solape EXACTO de {@code start_at}; los
     * parciales —10:00-10:30 contra 10:15-10:45— dependen solo de este orden.
     */
    @Nested
    @DisplayName("Serializacion contra la carrera")
    class Serializacion {

        @Test
        @DisplayName("toma el lock del veterinario antes de leer los solapes y de guardar")
        void toma_el_lock_antes_de_leer_los_solapes() {
            stubEmpleadoYSede();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre(null));

            InOrder orden = inOrder(employeeQueryPort, repository);
            orden.verify(employeeQueryPort).lockForOverlapCheck(EMPLOYEE, COMPANY);
            orden.verify(repository).findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(),
                    eq(DEFECTO), isNull());
            orden.verify(repository).save(any());
        }

        @Test
        @DisplayName("el lock es la primera sentencia: se toma incluso antes de resolver al veterinario")
        void el_lock_se_toma_antes_de_resolver_al_veterinario() {
            when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(contactoLibre(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found");

            InOrder orden = inOrder(employeeQueryPort);
            orden.verify(employeeQueryPort).lockForOverlapCheck(EMPLOYEE, COMPANY);
            orden.verify(employeeQueryPort).findByIdAndCompanyId(EMPLOYEE, COMPANY);
            verifyNoInteractions(repository, animalQueryPort, ownerQueryPort, branchQueryPort);
        }
    }

    /**
     * BE-17. Antes de este defecto el solape era un aviso: la cita se guardaba
     * igualmente y el conflicto se descubria con el animal en la sala. Ahora
     * bloquea con 409, y solo un {@code forceOverlap} explicito lo atraviesa.
     */
    @Nested
    @DisplayName("Bloqueo por solape")
    class BloqueoPorSolape {

        @Test
        @DisplayName("el solape impide agendar: lanza y no guarda nada")
        void el_solape_impide_agendar() {
            stubEmpleadoYSede();
            stubSolapeCon(List.of(70L));

            // Solo el tipo: el texto del detail y la lista de ids que lo acompañan son
            // material sensible (filtran agenda entre sedes) y estan cambiando. Lo
            // estable —y lo que de verdad arreglaba el defecto— es que no se guarde.
            assertThatThrownBy(() -> service.execute(contactoLibre(null)))
                    .isInstanceOf(AppointmentOverlapException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el correo de confirmacion no se encola cuando el solape bloquea")
        void el_correo_no_se_encola_cuando_el_solape_bloquea() {
            stubEmpleadoYSede();
            stubSolapeCon(List.of(70L));

            assertThatThrownBy(() -> service.execute(contactoLibre("walkin@example.com")))
                    .isInstanceOf(AppointmentOverlapException.class);

            // La comprobacion va ANTES de resolver el correo: si fuera al reves, el
            // cliente recibiria la confirmacion de una cita que se rechazo.
            verifyNoInteractions(confirmationEmailSender, companyQueryPort, appointmentMetrics);
        }

        @Test
        @DisplayName("con forceOverlap la cita si se guarda pese al cruce")
        void con_force_overlap_la_cita_si_se_guarda() {
            stubEmpleadoYSede();
            stubSolapeCon(List.of(70L, 71L));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AppointmentDto dto = service.execute(contactoLibre(null, true));

            assertThat(dto.status()).isEqualTo(AppointmentStatus.REQUESTED);
            verify(repository).save(any());
        }

        /**
         * Issue #240. Este test, con el repositorio doblado, nunca vera el defecto
         * completo —el que reventaba era el INSERT contra
         * {@code uq_appointments_active_employee_start}, y eso se prueba en
         * {@code AppointmentPersistenceIT}—. Lo que fija aqui es su mitad: que el
         * service deja marcada la decision en la cita, porque es lo unico que la base
         * puede mirar despues para distinguir este doble booking deliberado de la
         * carrera del #114.
         */
        @Test
        @DisplayName("el forzado viaja marcado en la cita que se guarda")
        void el_forzado_viaja_marcado_en_la_cita_que_se_guarda() {
            stubEmpleadoYSede();
            stubSolapeCon(List.of(70L));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(contactoLibre(null, true));

            ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().isOverlapForced()).isTrue();
        }

        @Test
        @DisplayName("forzar sobre una agenda libre no marca nada: no hay de que eximir")
        void forzar_sobre_una_agenda_libre_no_marca_nada() {
            stubEmpleadoYSede();
            stubGuardadoSinSolapes();

            service.execute(contactoLibre(null, true));

            ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
            verify(repository).save(guardada.capture());
            // Marcarla aqui la dejaria exenta del indice unico sin haber pisado a
            // nadie, y con ella su hueco entero desprotegido frente a la carrera.
            assertThat(guardada.getValue().isOverlapForced()).isFalse();
        }

        /**
         * El cruce se calcula por veterinario, pero solo se le puede contar al caller
         * lo de sus sedes. Bloquear y revelar son dos decisiones distintas.
         */
        @Test
        @DisplayName("un cruce en una sede del caller sale nombrado en la excepcion")
        void un_cruce_en_sede_propia_sale_nombrado() {
            stubEmpleadoYSede();
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
            when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                    isNull()))
                    .thenReturn(List.of(
                            new AppointmentRepository.Overlap(70L, AppointmentMother.BRANCH_ID)));

            assertThatThrownBy(() -> service.execute(contactoLibre(null)))
                    .isInstanceOf(AppointmentOverlapException.class)
                    .extracting(
                            e -> ((AppointmentOverlapException) e).getOverlappingAppointmentIds())
                    .isEqualTo(List.of(70L));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un cruce en una sede ajena bloquea igual pero no revela ningun id")
        void un_cruce_en_sede_ajena_no_revela_ningun_id() {
            stubEmpleadoYSede();
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
            when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                    isNull())).thenReturn(List.of(new AppointmentRepository.Overlap(70L, 999L)));

            // Fuga corregida: el veterinario esta ocupado en otra sede, asi que la cita
            // se bloquea, pero el caller no puede llegar a saber con que cita choca —a
            // base de 409 se reconstruia la agenda de la otra sede.
            assertThatThrownBy(() -> service.execute(contactoLibre(null)))
                    .isInstanceOf(AppointmentOverlapException.class)
                    .extracting(
                            e -> ((AppointmentOverlapException) e).getOverlappingAppointmentIds())
                    .isEqualTo(List.of());

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("con cruces mezclados solo se nombran los visibles")
        void con_cruces_mezclados_solo_se_nombran_los_visibles() {
            stubEmpleadoYSede();
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
            when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                    isNull()))
                    .thenReturn(List.of(
                            new AppointmentRepository.Overlap(70L, AppointmentMother.BRANCH_ID),
                            new AppointmentRepository.Overlap(71L, 999L)));

            assertThatThrownBy(() -> service.execute(contactoLibre(null)))
                    .isInstanceOf(AppointmentOverlapException.class)
                    .extracting(
                            e -> ((AppointmentOverlapException) e).getOverlappingAppointmentIds())
                    .isEqualTo(List.of(70L));
        }

        @Test
        @DisplayName("sin solape el DTO trae la lista vacia, nunca null")
        void sin_solape_el_dto_trae_la_lista_vacia() {
            stubEmpleadoYSede();
            stubGuardadoSinSolapes();

            AppointmentDto dto = service.execute(contactoLibre(null));

            assertThat(dto.overlappingAppointmentIds()).isEmpty();
        }

        @Test
        @DisplayName("la ventana de solape se calcula con la duracion por defecto de la empresa")
        void la_ventana_se_calcula_con_la_duracion_por_defecto() {
            stubEmpleadoYSede();
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(90);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), eq(AppointmentMother.INICIO),
                    eq(AppointmentMother.INICIO.plusMinutes(90)), eq(90), isNull()))
                    .thenReturn(List.of());

            service.execute(contactoLibre(null));

            // La cita no trae duracion propia, asi que el fin del intervalo lo pone la
            // empresa. Un endAt mal derivado consultaria la ventana equivocada y el
            // bloqueo dejaria pasar cruces reales.
            verify(repository).findOverlapping(COMPANY, EMPLOYEE, AppointmentMother.INICIO,
                    AppointmentMother.INICIO.plusMinutes(90), 90, null);
        }

        @Test
        @DisplayName("la duracion propia de la cita gana sobre la de la empresa al calcular el fin")
        void la_duracion_propia_gana_al_calcular_el_fin() {
            stubEmpleadoYSede();
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), eq(AppointmentMother.INICIO),
                    eq(AppointmentMother.INICIO.plusMinutes(15)), eq(DEFECTO), isNull()))
                    .thenReturn(List.of());

            service.execute(new CreateAppointmentCommand(AppointmentMother.INICIO, 15,
                    AppointmentType.GROOMING, EMPLOYEE, null, null, "Walk-in", null, null, null,
                    AppointmentMother.BRANCH_ID, COMPANY, false, AppointmentMother.SEDES_VISIBLES));

            verify(repository).findOverlapping(COMPANY, EMPLOYEE, AppointmentMother.INICIO,
                    AppointmentMother.INICIO.plusMinutes(15), DEFECTO, null);
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

        @Test
        @DisplayName("una excepcion del sender dentro del afterCommit no se propaga: la transaccion ya confirmo")
        void una_excepcion_del_sender_en_el_after_commit_no_se_propaga() {
            stubEmpleadoYSede();
            stubDatosDelCorreo();
            stubGuardadoSinSolapes();
            doThrow(new RuntimeException("Resend caido")).when(confirmationEmailSender).send(any());

            service.execute(contactoLibre("walkin@example.com"));

            // El callback la atrapa y solo la registra: una excepcion aqui se propagaria al
            // caller aunque la transaccion ya haya confirmado, y convertiria una cita
            // agendada correctamente en un 500.
            assertThatCode(this::confirmarLaTransaccion).doesNotThrowAnyException();
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
