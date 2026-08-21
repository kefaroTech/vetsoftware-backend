package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    @Mock
    private AppointmentDurationPolicyPort durationPolicyPort;
    @InjectMocks
    private UpdateAppointmentService service;

    private static final int DEFECTO = AppointmentMother.DURACION_POR_DEFECTO;

    private void stubCitaExistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
    }

    /** La agenda esta libre. El {@code excludeId} es la propia cita editada. */
    private void stubSinSolapes() {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                eq(ID))).thenReturn(List.of());
    }

    /**
     * Cruces en una sede que el caller ve, para que no los filtre la visibilidad.
     */
    private void stubSolapeCon(List<Long> ids) {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                eq(ID)))
                .thenReturn(ids.stream().map(
                        id -> new AppointmentRepository.Overlap(id, AppointmentMother.BRANCH_ID))
                        .toList());
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
        stubSinSolapes();

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
        stubSinSolapes();

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeActualizacion());

        assertThat(dto.branch().id()).isEqualTo(AppointmentMother.BRANCH_ID);
        assertThat(dto.status()).isEqualTo(AppointmentStatus.REQUESTED);
    }

    @Test
    @DisplayName("el solape recalculado con la hora nueva bloquea la edicion y no guarda")
    void el_solape_recalculado_bloquea_la_edicion() {
        stubCitaExistente();
        stubReferenciasCompletas();
        stubSolapeCon(List.of(81L));

        // Sin afirmar sobre el detail: su texto y los ids que publica estan cambiando
        // por el hallazgo de fuga de agenda entre sedes.
        assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeActualizacion()))
                .isInstanceOf(AppointmentOverlapException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("con forceOverlap la edicion se guarda pese al cruce")
    void con_force_overlap_la_edicion_se_guarda() {
        stubCitaExistente();
        stubReferenciasCompletas();
        stubSolapeCon(List.of(81L));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeActualizacion(null, true));

        assertThat(dto.status()).isEqualTo(AppointmentStatus.REQUESTED);
        verify(repository).save(any());
    }

    /**
     * Issue #240. Con el repositorio doblado este caso no llega al indice unico
     * —eso lo prueba {@code AppointmentPersistenceIT}—; fija la mitad que le toca
     * al service: dejar la decision marcada en la cita para que la fila resultante
     * no compita por el hueco.
     */
    @Test
    @DisplayName("el forzado viaja marcado en la cita editada")
    void el_forzado_viaja_marcado_en_la_cita_editada() {
        stubCitaExistente();
        stubReferenciasCompletas();
        stubSolapeCon(List.of(81L));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(AppointmentMother.comandoDeActualizacion(null, true));

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().isOverlapForced()).isTrue();
    }

    @Test
    @DisplayName("editar hacia un hueco libre le devuelve la reserva a la cita")
    void editar_hacia_un_hueco_libre_le_devuelve_la_reserva() {
        stubCitaExistente();
        stubReferenciasCompletas();
        stubSinSolapes();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // El PUT llega con forceOverlap, pero ya no hay nada que pisar. Si la marca
        // se quedara pegada, esta cita quedaria exenta del indice para siempre y su
        // hueco nuevo admitiria la carrera que el #114 cerro.
        service.execute(AppointmentMother.comandoDeActualizacion(null, true));

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().isOverlapForced()).isFalse();
    }

    @Test
    @DisplayName("la cita editada se excluye de su propia consulta de solapes")
    void la_cita_editada_se_excluye_de_su_propia_consulta() {
        stubCitaExistente();
        stubReferenciasCompletas();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        service.execute(AppointmentMother.comandoDeActualizacion());

        // Sin el excludeId la cita chocaria consigo misma y ninguna edicion de notas
        // podria guardarse jamas.
        verify(repository).findOverlapping(COMPANY, EMPLOYEE, AppointmentMother.NUEVO_INICIO,
                AppointmentMother.NUEVO_INICIO.plusMinutes(DEFECTO), DEFECTO, ID);
    }

    @Test
    @DisplayName("el PUT sin duracion devuelve la cita al valor por defecto de la empresa")
    void el_put_sin_duracion_vuelve_al_valor_por_defecto() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conDuracion(90)));
        stubReferenciasCompletas();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeActualizacion());

        // Es un reemplazo completo: omitir la duracion significa "vuelve al default",
        // no "no la toques" — eso ultimo es el PATCH de reschedule.
        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getDurationMinutes()).isNull();
        assertThat(dto.durationMinutes()).isNull();
    }

    @Test
    @DisplayName("el PUT con duracion la guarda y la usa para acotar la ventana de solape")
    void el_put_con_duracion_la_guarda() {
        stubCitaExistente();
        stubReferenciasCompletas();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                eq(ID))).thenReturn(List.of());

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeActualizacion(120, false));

        assertThat(dto.durationMinutes()).isEqualTo(120);
        verify(repository).findOverlapping(COMPANY, EMPLOYEE, AppointmentMother.NUEVO_INICIO,
                AppointmentMother.NUEVO_INICIO.plusMinutes(120), DEFECTO, ID);
    }

    @Test
    @DisplayName("una duracion invalida rompe la invariante del dominio y no se guarda")
    void una_duracion_invalida_no_se_guarda() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(animalQueryPort.findByIdAndCompanyId(AppointmentMother.ANIMAL_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.FIRULAIS));
        when(ownerQueryPort.findByIdAndCompanyId(AppointmentMother.OWNER_ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.DUENO));

        assertThatThrownBy(
                () -> service.execute(AppointmentMother.comandoDeActualizacion(0, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be greater than 0");

        verify(repository, never()).save(any());
        verifyNoInteractions(durationPolicyPort);
    }

    @Test
    @DisplayName("convierte la cita en contacto libre cuando se quitan animal y propietario")
    void convierte_la_cita_en_contacto_libre() {
        stubCitaExistente();
        when(employeeQueryPort.findByIdAndCompanyId(EMPLOYEE, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.VETERINARIA));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        AppointmentDto dto = service.execute(new UpdateAppointmentCommand(ID,
                AppointmentMother.NUEVO_INICIO, null, AppointmentType.GROOMING, EMPLOYEE, null,
                null, "Walk-in", "3001234567", "walkin@example.com", null, COMPANY, false,
                AppointmentMother.SEDES_VISIBLES));

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
        // El lock del #241 se toma antes de saber si la cita existe -es la primera
        // sentencia-, asi que aqui el puerto del empleado SI se toca; lo que no
        // puede haber es lectura del veterinario ni escritura de nada.
        verify(employeeQueryPort).lockForOverlapCheck(EMPLOYEE, COMPANY);
        verify(employeeQueryPort, never()).findByIdAndCompanyId(any(), any());
        verifyNoInteractions(animalQueryPort, ownerQueryPort);
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
                AppointmentMother.NUEVO_INICIO, null, AppointmentType.GROOMING, EMPLOYEE, null,
                null, null, null, null, null, COMPANY, false, AppointmentMother.SEDES_VISIBLES)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one of");

        verify(repository, never()).save(any());
    }

    /**
     * Issue #241. El lock del #114 cerro la carrera solo en el alta; editar seguia
     * leyendo la agenda y guardando sin serializar, asi que dos ediciones
     * concurrentes sobre el mismo veterinario podian dejar dos duenos a la misma
     * hora. Y sin senal ninguna: 200 en las dos peticiones, sin excepcion y con
     * {@code overlappingAppointmentIds} vacio en las dos, que es justo lo que el
     * front lee como "no habia cruce".
     *
     * <p>
     * Desde el #240 la base tampoco lo tapa: una cita forzada renuncia a su hueco
     * en {@code uq_appointments_active_employee_start}, asi que ni el solape exacto
     * contra ella se frena abajo. Quien arbitra es el {@code findOverlapping} de
     * este servicio, y solo vale serializado.
     *
     * <p>
     * Aqui se fija el ORDEN de las sentencias, que es lo unico que un refactor
     * puede romper en silencio —las llamadas siguen todas ahi, solo cambian de
     * sitio—. Que el lock bloquee de verdad lo prueba la base en
     * {@code AppointmentPersistenceIT}; la carrera con dos hilos reales es el #225.
     */
    @Nested
    @DisplayName("Serializacion contra la carrera (issue #241)")
    class Serializacion {

        @Test
        @DisplayName("toma el lock del veterinario antes de leer los solapes y de guardar")
        void toma_el_lock_antes_de_leer_los_solapes() {
            stubCitaExistente();
            stubReferenciasCompletas();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            stubSinSolapes();

            service.execute(AppointmentMother.comandoDeActualizacion());

            InOrder orden = inOrder(employeeQueryPort, repository);
            orden.verify(employeeQueryPort).lockForOverlapCheck(EMPLOYEE, COMPANY);
            orden.verify(repository).findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(),
                    eq(DEFECTO), eq(ID));
            orden.verify(repository).save(any());
        }

        @Test
        @DisplayName("el lock es la primera sentencia: se toma incluso antes de leer la cita")
        void el_lock_se_toma_antes_de_leer_la_cita() {
            when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(AppointmentMother.comandoDeActualizacion()))
                    .isInstanceOf(AppointmentNotFoundException.class);

            // Detras de la lectura la ventana vuelve a abrirse entera: la transaccion
            // rival ya habria leido la agenda antes de ponerse a esperar a nadie.
            InOrder orden = inOrder(employeeQueryPort, repository);
            orden.verify(employeeQueryPort).lockForOverlapCheck(EMPLOYEE, COMPANY);
            orden.verify(repository).findByIdAndCompanyId(ID, COMPANY);
        }

        @Test
        @DisplayName("bloquea al veterinario destino del PUT, y solo a ese")
        void bloquea_solo_al_veterinario_destino() {
            stubCitaExistente();
            stubReferenciasCompletas();
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            stubSinSolapes();

            service.execute(AppointmentMother.comandoDeActualizacion());

            // Un unico lock por transaccion. Es lo que hace imposible el interbloqueo
            // cruzado del #229: una transaccion que solo toma un lock nunca espera
            // teniendo cogido otro, asi que no puede formar parte de un ciclo. Si
            // alguien anade el lock del veterinario de origen, hara falta leer la cita
            // antes del primer lock y un orden total sobre los dos ids.
            verify(employeeQueryPort, times(1)).lockForOverlapCheck(any(), any());
        }
    }

    /**
     * BE-17. El cruce se calcula por empresa + veterinario, sin sede, pero el
     * listado de citas si esta acotado por sede: devolver en el 409 el id de una
     * cita de otra sede la hacia legible entera por {@code GET /appointments/{id}},
     * que solo comprueba la empresa. Editar es tan explotable como agendar para ese
     * barrido, asi que la redaccion se verifica aqui igual que en
     * {@code CreateAppointmentServiceTest}.
     *
     * <p>
     * Se afirma sobre el <b>estado</b> de la excepcion —ids visibles, recuento, si
     * el nombre del veterinario sale o no—, nunca sobre el texto literal del
     * detail: la redaccion es la regla, la cadena es un detalle que cambiara.
     */
    @Nested
    @DisplayName("Redaccion del 409 segun el alcance de sede")
    class RedaccionPorSede {

        /** Sede fuera del alcance del caller: no esta en SEDES_VISIBLES. */
        private static final Long SEDE_AJENA = 999L;
        /** Cita agendada en esa sede. Su id no puede asomar por ninguna parte. */
        private static final Long CITA_AJENA = 8877L;
        private static final Long CITA_PROPIA = 81L;

        private void stubSolapesEn(List<AppointmentRepository.Overlap> overlaps) {
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
            when(repository.findOverlapping(eq(COMPANY), eq(EMPLOYEE), any(), any(), eq(DEFECTO),
                    eq(ID))).thenReturn(overlaps);
        }

        private AppointmentOverlapException solapeAlEditar(UpdateAppointmentCommand comando) {
            Throwable fallo = catchThrowable(() -> service.execute(comando));
            assertThat(fallo).isInstanceOf(AppointmentOverlapException.class);
            return (AppointmentOverlapException) fallo;
        }

        /** Como se redacta el mismo cruce cuando el ajeno ni siquiera existe. */
        private String redaccionSoloConLaVisible(int minutosDeVentana) {
            return new AppointmentOverlapException(EMPLOYEE,
                    AppointmentMother.OTRO_VETERINARIO.name(), AppointmentMother.NUEVO_INICIO,
                    AppointmentMother.NUEVO_INICIO.plusMinutes(minutosDeVentana),
                    List.of(CITA_PROPIA), 1).getMessage();
        }

        @Test
        @DisplayName("un cruce en una sede del caller sale nombrado, con veterinario e ids")
        void un_cruce_en_sede_propia_sale_nombrado() {
            stubCitaExistente();
            stubReferenciasCompletas();
            stubSolapesEn(List.of(
                    new AppointmentRepository.Overlap(CITA_PROPIA, AppointmentMother.BRANCH_ID)));

            AppointmentOverlapException solape = solapeAlEditar(
                    AppointmentMother.comandoDeActualizacion());

            assertThat(solape.getOverlappingAppointmentIds()).containsExactly(CITA_PROPIA);
            assertThat(solape.getOverlapCount()).isEqualTo(1);
            assertThat(solape.getEmployeeId()).isEqualTo(EMPLOYEE);
            // El nombre solo se publica cuando hay algo visible que explicar.
            assertThat(solape.getMessage()).contains(AppointmentMother.OTRO_VETERINARIO.name());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un cruce en una sede ajena bloquea igual pero no revela id ni veterinario")
        void un_cruce_en_sede_ajena_no_revela_nada() {
            stubCitaExistente();
            stubReferenciasCompletas();
            stubSolapesEn(List.of(new AppointmentRepository.Overlap(CITA_AJENA, SEDE_AJENA)));

            AppointmentOverlapException solape = solapeAlEditar(
                    AppointmentMother.comandoDeActualizacion());

            assertThat(solape.getOverlappingAppointmentIds()).isEmpty();
            // Ni el id de la cita ajena ni el nombre del veterinario: con el id, un
            // GET /appointments/{id} entregaba la ficha completa del cliente.
            assertThat(solape.getMessage()).doesNotContain(String.valueOf(CITA_AJENA))
                    .doesNotContain(AppointmentMother.OTRO_VETERINARIO.name());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("con cruces mezclados publica los visibles y ni el id ni el numero de los demas")
        void con_cruces_mezclados_no_filtra_el_recuento() {
            stubCitaExistente();
            stubReferenciasCompletas();
            stubSolapesEn(List.of(
                    new AppointmentRepository.Overlap(CITA_PROPIA, AppointmentMother.BRANCH_ID),
                    new AppointmentRepository.Overlap(CITA_AJENA, SEDE_AJENA)));

            AppointmentOverlapException solape = solapeAlEditar(
                    AppointmentMother.comandoDeActualizacion());

            assertThat(solape.getOverlappingAppointmentIds()).containsExactly(CITA_PROPIA);
            // El total sigue existiendo, pero es canal de log —el handler lo saca por
            // el warn— y nunca respuesta.
            assertThat(solape.getOverlapCount()).isEqualTo(2);
            // Y lo que ve el caller tiene que ser INDISTINGUIBLE del escenario en el que
            // solo existiera el cruce visible: si asomara un "y 1 mas", un hueco o un
            // total, el barrido de la agenda ajena seguiria siendo posible aunque los
            // ids fueran recortados. No se fija el texto: se compara contra el que
            // produce ese mismo caso ya redactado.
            assertThat(solape.getMessage()).isEqualTo(redaccionSoloConLaVisible(DEFECTO))
                    .doesNotContain(String.valueOf(CITA_AJENA));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("el PUT sin duracion redacta el 409 sobre la ventana devuelta al default")
        void el_put_sin_duracion_redacta_sobre_la_ventana_por_defecto() {
            when(repository.findByIdAndCompanyId(ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.conDuracion(90)));
            stubReferenciasCompletas();
            stubSolapesEn(List.of(
                    new AppointmentRepository.Overlap(CITA_PROPIA, AppointmentMother.BRANCH_ID)));

            AppointmentOverlapException solape = solapeAlEditar(
                    AppointmentMother.comandoDeActualizacion());

            // El PUT es un reemplazo: omitir la duracion devuelve la cita al default de
            // la empresa, asi que el intervalo que se le cuenta al caller son 30 minutos
            // y no los 90 que la cita tenia. Con la ventana equivocada el aviso describe
            // un choque distinto del que se comprobo.
            assertThat(solape.getMessage()).isEqualTo(redaccionSoloConLaVisible(DEFECTO));
            verify(repository, never()).save(any());
        }
    }
}
