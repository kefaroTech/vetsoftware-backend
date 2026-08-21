package com.vetsoftware.app.appointment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.out.AppointmentDurationPolicyPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.AppointmentOverlapException;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
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
@DisplayName("RescheduleAppointmentService — mover una cita de hora o de veterinario")
class RescheduleAppointmentServiceTest {

    private static final Long COMPANY = AppointmentMother.COMPANY_ID;
    private static final Long ID = AppointmentMother.APPOINTMENT_ID;
    private static final Long OTRO_EMPLEADO = 5L;

    @Mock
    private AppointmentRepository repository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private AppointmentDurationPolicyPort durationPolicyPort;
    @InjectMocks
    private RescheduleAppointmentService service;

    private static final int DEFECTO = AppointmentMother.DURACION_POR_DEFECTO;

    private static RescheduleAppointmentCommand comando() {
        return AppointmentMother.comandoDeReprogramacion();
    }

    private void stubSinSolapes() {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.findOverlapping(eq(COMPANY), eq(OTRO_EMPLEADO), any(), any(), eq(DEFECTO),
                eq(ID))).thenReturn(List.of());
    }

    /**
     * Cruces en una sede que el caller ve, para que no los filtre la visibilidad.
     */
    private void stubSolapeCon(List<Long> ids) {
        when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
        when(repository.findOverlapping(eq(COMPANY), eq(OTRO_EMPLEADO), any(), any(), eq(DEFECTO),
                eq(ID)))
                .thenReturn(ids.stream().map(
                        id -> new AppointmentRepository.Overlap(id, AppointmentMother.BRANCH_ID))
                        .toList());
    }

    @Test
    @DisplayName("guarda la cita con la hora y el veterinario nuevos, sin tocar el estado")
    void guarda_la_cita_con_la_hora_y_el_veterinario_nuevos() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conEstado(AppointmentStatus.CONFIRMED)));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        service.execute(comando());

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        Appointment cita = guardada.getValue();
        assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.NUEVO_INICIO);
        assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.OTRO_VETERINARIO);
        assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("el cruce con la agenda del veterinario nuevo bloquea la reprogramacion")
    void el_cruce_con_el_veterinario_nuevo_bloquea() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        stubSolapeCon(List.of(90L, 91L));

        // Sin afirmar sobre el detail: su texto y los ids que publica estan cambiando
        // por el hallazgo de fuga de agenda entre sedes.
        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(AppointmentOverlapException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("con forceOverlap la reprogramacion se guarda pese al cruce")
    void con_force_overlap_la_reprogramacion_se_guarda() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        stubSolapeCon(List.of(90L, 91L));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeReprogramacion(null, true));

        assertThat(dto.startAt()).isEqualTo(AppointmentMother.NUEVO_INICIO);
        verify(repository).save(any());
    }

    /**
     * Issue #240. Reprogramar encima de otra cita es la operacion mas comun de una
     * recepcion que encaja una urgencia, y era la que moria contra
     * {@code uq_appointments_active_employee_start}. Aqui se fija que el service
     * deja la marca; que la fila marcada si entre en la base lo prueba
     * {@code AppointmentPersistenceIT}.
     */
    @Test
    @DisplayName("el forzado viaja marcado en la cita reprogramada")
    void el_forzado_viaja_marcado_en_la_cita_reprogramada() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        stubSolapeCon(List.of(90L));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(AppointmentMother.comandoDeReprogramacion(null, true));

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().isOverlapForced()).isTrue();
    }

    @Test
    @DisplayName("mover la cita a un hueco libre le devuelve la reserva")
    void mover_la_cita_a_un_hueco_libre_le_devuelve_la_reserva() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        stubSinSolapes();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Sacar una cita de un hueco compartido la devuelve a competir por el suyo:
        // la exencion del indice dura lo que dura el solape, no para siempre.
        service.execute(AppointmentMother.comandoDeReprogramacion(null, true));

        ArgumentCaptor<Appointment> guardada = ArgumentCaptor.forClass(Appointment.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().isOverlapForced()).isFalse();
    }

    @Test
    @DisplayName("el PATCH sin duracion CONSERVA la que la cita ya tenia")
    void el_patch_sin_duracion_conserva_la_actual() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conDuracion(90)));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        AppointmentDto dto = service.execute(comando());

        // Al contrario que el PUT: quien mueve la cita de hora no esta renunciando a
        // la duracion pactada. La ventana de solape se calcula con esos 90 minutos.
        assertThat(dto.durationMinutes()).isEqualTo(90);
        verify(repository).findOverlapping(COMPANY, OTRO_EMPLEADO, AppointmentMother.NUEVO_INICIO,
                AppointmentMother.NUEVO_INICIO.plusMinutes(90), DEFECTO, ID);
    }

    @Test
    @DisplayName("el PATCH con duracion nueva la reemplaza")
    void el_patch_con_duracion_nueva_la_reemplaza() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.conDuracion(90)));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        AppointmentDto dto = service.execute(AppointmentMother.comandoDeReprogramacion(20, false));

        assertThat(dto.durationMinutes()).isEqualTo(20);
    }

    @Test
    @DisplayName("una cita sin duracion propia hereda la de la empresa para acotar la ventana")
    void una_cita_sin_duracion_propia_hereda_la_de_la_empresa() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        stubSinSolapes();

        service.execute(comando());

        verify(repository).findOverlapping(COMPANY, OTRO_EMPLEADO, AppointmentMother.NUEVO_INICIO,
                AppointmentMother.NUEVO_INICIO.plusMinutes(DEFECTO), DEFECTO, ID);
    }

    @Test
    @DisplayName("una cita de otra empresa es inexistente y no se escribe nada")
    void una_cita_de_otra_empresa_es_inexistente() {
        when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(AppointmentNotFoundException.class)
                .hasMessageContaining("Appointment not found: 55");

        verify(repository, never()).save(any());
        // El lock del #241 es la primera sentencia y se toma antes de saber si la
        // cita existe: el puerto del empleado se toca, pero solo para bloquear.
        verify(employeeQueryPort).lockForOverlapCheck(OTRO_EMPLEADO, COMPANY);
        verify(employeeQueryPort, never()).findByIdAndCompanyId(any(), any());
    }

    @Test
    @DisplayName("no reprograma sobre un veterinario de otra empresa")
    void no_reprograma_sobre_un_veterinario_de_otra_empresa() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Employee not found: 5");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("propaga la invariante del dominio si la hora nueva viene vacia")
    void propaga_la_invariante_si_la_hora_nueva_viene_vacia() {
        when(repository.findByIdAndCompanyId(ID, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.solicitada()));
        when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));

        assertThatThrownBy(() -> service.execute(new RescheduleAppointmentCommand(ID, null, null,
                OTRO_EMPLEADO, COMPANY, false, AppointmentMother.SEDES_VISIBLES)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startAt is required");

        verify(repository, never()).save(any());
    }

    /**
     * Issue #241. La carrera del #114 nunca fue exclusiva del alta: reprogramar
     * hace el mismo leer-y-escribir —{@code findOverlapping} y despues
     * {@code save}— y hasta este arreglo no serializaba nada. Dos reprogramaciones
     * simultaneas sobre la misma agenda leian las dos un hueco libre y guardaban
     * las dos, con dos duenos citados a la misma hora y sin ninguna senal: 200 en
     * las dos peticiones y {@code overlappingAppointmentIds} vacio en las dos.
     *
     * <p>
     * Desde el #240 esto pesa mas: una cita forzada renuncia a su hueco en
     * {@code uq_appointments_active_employee_start}, asi que la base ya no atrapa
     * ni siquiera el solape exacto contra ella. El unico arbitro que queda es este
     * orden de sentencias.
     *
     * <p>
     * Lo que fija esta clase es el ORDEN, contra dobles: que el lock llegue antes
     * de la lectura. Que el lock bloquee de verdad es cosa de la base y se prueba
     * en {@code AppointmentPersistenceIT}; la carrera con dos hilos reales sigue
     * siendo el issue #225.
     */
    @Nested
    @DisplayName("Serializacion contra la carrera (issue #241)")
    class Serializacion {

        @Test
        @DisplayName("toma el lock del veterinario antes de leer los solapes y de guardar")
        void toma_el_lock_antes_de_leer_los_solapes() {
            when(repository.findByIdAndCompanyId(ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.solicitada()));
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            stubSinSolapes();

            service.execute(comando());

            InOrder orden = inOrder(employeeQueryPort, repository);
            orden.verify(employeeQueryPort).lockForOverlapCheck(OTRO_EMPLEADO, COMPANY);
            orden.verify(repository).findOverlapping(eq(COMPANY), eq(OTRO_EMPLEADO), any(), any(),
                    eq(DEFECTO), eq(ID));
            orden.verify(repository).save(any());
        }

        @Test
        @DisplayName("el lock es la primera sentencia: se toma incluso antes de leer la cita")
        void el_lock_se_toma_antes_de_leer_la_cita() {
            when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(AppointmentNotFoundException.class);

            // Detras de la lectura de la cita la ventana vuelve a abrirse entera: la
            // transaccion rival ya habria leido la agenda antes de esperar a nadie.
            InOrder orden = inOrder(employeeQueryPort, repository);
            orden.verify(employeeQueryPort).lockForOverlapCheck(OTRO_EMPLEADO, COMPANY);
            orden.verify(repository).findByIdAndCompanyId(ID, COMPANY);
        }

        @Test
        @DisplayName("bloquea al veterinario destino, no al que la cita tenia antes")
        void bloquea_al_veterinario_destino() {
            when(repository.findByIdAndCompanyId(ID, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.solicitada()));
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            stubSinSolapes();

            // La cita esta en la agenda de AppointmentMother.EMPLOYEE_ID y se mueve a la
            // del suplente. Se bloquea la de DESTINO y solo esa: es la unica que gana
            // una cita, y con un solo lock por transaccion no hay interbloqueo cruzado
            // que ordenar (#229). Si alguien anade aqui el lock del origen, que sea
            // sabiendo que hay que leer la cita antes del primer lock y que hace falta
            // un orden total.
            service.execute(comando());

            verify(employeeQueryPort).lockForOverlapCheck(OTRO_EMPLEADO, COMPANY);
            verify(employeeQueryPort, never()).lockForOverlapCheck(AppointmentMother.EMPLOYEE_ID,
                    COMPANY);
        }
    }

    /**
     * BE-17. Reprogramar barre la agenda ajena igual de bien que agendar: se pide
     * la misma hora una y otra vez y el 409 va contestando. Por eso la redaccion
     * —que se comprueba exhaustivamente en {@code AppointmentOverlapsTest} y en
     * {@code CreateAppointmentServiceTest}— tambien tiene que estar verificada en
     * este verbo, no solo el bloqueo.
     *
     * <p>
     * Se afirma sobre el <b>estado</b> de la excepcion —ids visibles, recuento, si
     * el nombre del veterinario sale o no—, nunca sobre el texto literal del
     * detail.
     */
    @Nested
    @DisplayName("Redaccion del 409 segun el alcance de sede")
    class RedaccionPorSede {

        /** Sede fuera del alcance del caller: no esta en SEDES_VISIBLES. */
        private static final Long SEDE_AJENA = 999L;
        /** Cita agendada en esa sede. Su id no puede asomar por ninguna parte. */
        private static final Long CITA_AJENA = 8877L;
        private static final Long CITA_PROPIA = 90L;

        private void stubSolapesEn(List<AppointmentRepository.Overlap> overlaps) {
            when(durationPolicyPort.defaultDurationMinutes(COMPANY)).thenReturn(DEFECTO);
            when(repository.findOverlapping(eq(COMPANY), eq(OTRO_EMPLEADO), any(), any(),
                    eq(DEFECTO), eq(ID))).thenReturn(overlaps);
        }

        private void stubCitaYVeterinario(Appointment cita) {
            when(repository.findByIdAndCompanyId(ID, COMPANY)).thenReturn(Optional.of(cita));
            when(employeeQueryPort.findByIdAndCompanyId(OTRO_EMPLEADO, COMPANY))
                    .thenReturn(Optional.of(AppointmentMother.OTRO_VETERINARIO));
        }

        private AppointmentOverlapException solapeAlReprogramar(
                RescheduleAppointmentCommand orden) {
            Throwable fallo = catchThrowable(() -> service.execute(orden));
            assertThat(fallo).isInstanceOf(AppointmentOverlapException.class);
            return (AppointmentOverlapException) fallo;
        }

        /** Como se redacta el mismo cruce cuando el ajeno ni siquiera existe. */
        private String redaccionSoloConLaVisible(int minutosDeVentana) {
            return new AppointmentOverlapException(OTRO_EMPLEADO,
                    AppointmentMother.OTRO_VETERINARIO.name(), AppointmentMother.NUEVO_INICIO,
                    AppointmentMother.NUEVO_INICIO.plusMinutes(minutosDeVentana),
                    List.of(CITA_PROPIA), 1).getMessage();
        }

        @Test
        @DisplayName("un cruce en una sede del caller sale nombrado, con veterinario e ids")
        void un_cruce_en_sede_propia_sale_nombrado() {
            stubCitaYVeterinario(AppointmentMother.solicitada());
            stubSolapesEn(List.of(
                    new AppointmentRepository.Overlap(CITA_PROPIA, AppointmentMother.BRANCH_ID)));

            AppointmentOverlapException solape = solapeAlReprogramar(comando());

            assertThat(solape.getOverlappingAppointmentIds()).containsExactly(CITA_PROPIA);
            assertThat(solape.getOverlapCount()).isEqualTo(1);
            assertThat(solape.getEmployeeId()).isEqualTo(OTRO_EMPLEADO);
            // El nombre solo se publica cuando hay algo visible que explicar.
            assertThat(solape.getMessage()).contains(AppointmentMother.OTRO_VETERINARIO.name());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un cruce en una sede ajena bloquea igual pero no revela id ni veterinario")
        void un_cruce_en_sede_ajena_no_revela_nada() {
            stubCitaYVeterinario(AppointmentMother.solicitada());
            stubSolapesEn(List.of(new AppointmentRepository.Overlap(CITA_AJENA, SEDE_AJENA)));

            AppointmentOverlapException solape = solapeAlReprogramar(comando());

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
            stubCitaYVeterinario(AppointmentMother.solicitada());
            stubSolapesEn(List.of(
                    new AppointmentRepository.Overlap(CITA_PROPIA, AppointmentMother.BRANCH_ID),
                    new AppointmentRepository.Overlap(CITA_AJENA, SEDE_AJENA)));

            AppointmentOverlapException solape = solapeAlReprogramar(comando());

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
        @DisplayName("el PATCH sin duracion redacta el 409 sobre la ventana que la cita conserva")
        void el_patch_sin_duracion_redacta_sobre_la_ventana_conservada() {
            stubCitaYVeterinario(AppointmentMother.conDuracion(90));
            stubSolapesEn(List.of(
                    new AppointmentRepository.Overlap(CITA_PROPIA, AppointmentMother.BRANCH_ID)));

            AppointmentOverlapException solape = solapeAlReprogramar(comando());

            // Al reves que el PUT: mover la cita de hora no renuncia a la duracion
            // pactada, asi que el intervalo que se le cuenta al caller son los 90
            // minutos de la cita y no los 30 del default de la empresa.
            assertThat(solape.getMessage()).isEqualTo(redaccionSoloConLaVisible(90))
                    .isNotEqualTo(redaccionSoloConLaVisible(DEFECTO));
            verify(repository, never()).save(any());
        }
    }
}
