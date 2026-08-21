package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.CompanyRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las citas contra MySQL real (BE-10 + BE-17).
 *
 * <p>
 * <b>Por que existe y por que urgia.</b> La consulta
 * {@code findOverlapCandidates} es JPQL nueva y Spring Data la parsea al crear
 * el bean, no al compilar: hasta que alguien la ejecuta contra una base, nadie
 * sabe si la aplicacion arranca siquiera. Esta clase es lo que cierra ese
 * riesgo — si la JPQL estuviera mal, el contexto no levantaria y todos los
 * casos de aqui fallarian a la vez.
 *
 * <p>
 * <b>El semiabierto es el corazon del asunto.</b> La interseccion se decide en
 * dos sitios que tienen que estar de acuerdo: la consulta acota la ventana
 * ({@code a.startAt < :endAt}, estricto) y {@code JpaAppointmentRepository}
 * resuelve el cruce exacto en Java. Si cualquiera de los dos cerrara el
 * intervalo, una agenda encadenada —10:00-10:30 seguida de 10:30-11:00— daria
 * conflicto en cada hueco y el bloqueo entero seria inusable. La matriz de
 * {@link Semiabierto} fija los dos bordes.
 *
 * <p>
 * Las citas se siembran por SQL nativo: lo que se prueba es la consulta contra
 * la tabla, no el camino de escritura (que tiene su propio caso en
 * {@link Escritura}).
 */
@Import({JpaAppointmentRepository.class, AppointmentJpaMapper.class})
@DisplayName("JpaAppointmentRepository — solapes y citas contra MySQL real")
class AppointmentPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long SEDE = SchemaSeed.BRANCH_ID;
    private static final Long VETERINARIA = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_VETERINARIO = SchemaSeed.OTRO_EMPLOYEE_ID;

    /** Padres propios de esta rodaja (ids 960+, fuera del rango de SchemaSeed). */
    private static final Long VETERINARIO_AJENO = 960L;
    private static final Long SEDE_AJENA = 961L;

    private static final LocalDate DIA = LocalDate.of(2026, 8, 1);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 7, 20, 8, 15);

    /** Duracion por defecto de la empresa que llega ya resuelta al puerto. */
    private static final int DEFECTO = 30;

    private long siguienteId = 1_000L;

    @Autowired
    private JpaAppointmentRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        // La empresa ajena de SchemaSeed no trae ni sede ni empleado propios, y los
        // necesito para probar que la consulta filtra por empresa con datos legitimos
        // en vez de con una fila imposible.
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede ajena', 'AJENA', :ciudad, :empresa)
                """).setParameter("id", SEDE_AJENA).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", OTRA_EMPRESA).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO employees (id, employee_code, hash_password, name, email,
                                              company_id)
                VALUES (:id, 'EMP-960', 'x', 'Vet ajeno', 'ajeno@test.local', :empresa)
                """).setParameter("id", VETERINARIO_AJENO).setParameter("empresa", OTRA_EMPRESA)
                .executeUpdate();
        entityManager.flush();

        // Guardia de la siembra. Todo lo de arriba va con INSERT IGNORE y MySQL degrada
        // a warning tanto el NOT NULL sin valor como la violacion de FK: la fila no se
        // inserta y nadie se entera. Sin esto el sintoma aparece mas abajo, como una FK
        // rota al insertar la cita, y apunta a la tabla equivocada.
        assertThat(filas("companies", EMPRESA)).as("la empresa de las citas").isOne();
        assertThat(filas("branches", SEDE)).as("la sede de las citas").isOne();
        assertThat(filas("employees", VETERINARIA)).as("el veterinario de las citas").isOne();
        assertThat(filas("employees", OTRO_VETERINARIO)).as("el segundo veterinario").isOne();
        assertThat(filas("branches", SEDE_AJENA)).as("la sede de la empresa ajena").isOne();
        assertThat(filas("employees", VETERINARIO_AJENO)).as("el veterinario ajeno").isOne();
    }

    private long filas(String tabla, Long id) {
        Number total = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .setParameter("id", id).getSingleResult();
        return total.longValue();
    }

    private static LocalDateTime hora(String hhmm) {
        return LocalDateTime.of(DIA, LocalTime.parse(hhmm));
    }

    /** Cita ya persistida, con los valores de la empresa y el vet por defecto. */
    private Long cita(String inicio, Integer duracion) {
        return cita(inicio, duracion, AppointmentStatus.CONFIRMED, EMPRESA, VETERINARIA, SEDE);
    }

    private Long cita(String inicio, Integer duracion, AppointmentStatus estado, Long empresa,
            Long empleado, Long sede) {
        return cita(inicio, duracion, estado, empresa, empleado, sede, false);
    }

    /**
     * @param forzada
     *            la cita se agendo sabiendo que pisaba a otra (issue #240). Es lo
     *            que el marcador generado mira para decidir si esta fila reserva su
     *            hueco.
     */
    private Long cita(String inicio, Integer duracion, AppointmentStatus estado, Long empresa,
            Long empleado, Long sede, boolean forzada) {
        Long id = siguienteId++;
        entityManager.createNativeQuery("""
                INSERT INTO appointments (id, start_at, duration_minutes, type, status, notes,
                                          employee_id, company_id, branch_id, version, enabled,
                                          overlap_forced, created_date)
                VALUES (:id, :inicio, :duracion, 'CONSULTATION', :estado, 'Sembrada', :empleado,
                        :empresa, :sede, 0, true, :forzada, :creada)
                """).setParameter("id", id).setParameter("inicio", hora(inicio))
                .setParameter("duracion", duracion).setParameter("estado", estado.name())
                .setParameter("empleado", empleado).setParameter("empresa", empresa)
                .setParameter("sede", sede).setParameter("forzada", forzada)
                .setParameter("creada", CREADA).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return id;
    }

    /** Cuenta las filas de una condicion sobre appointments, sin leer el valor. */
    private long citasQue(String condicion, Long id) {
        Number total = (Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM appointments WHERE id = :id AND " + condicion)
                .setParameter("id", id).getSingleResult();
        return total.longValue();
    }

    /** Busca cruces para el intervalo dado, con la empresa y el vet por defecto. */
    private List<Long> solapes(String inicio, int duracion) {
        return solapes(inicio, duracion, DEFECTO, null);
    }

    private List<Long> solapes(String inicio, int duracion, int defecto, Long excluida) {
        return cruces(inicio, duracion, defecto, excluida).stream()
                .map(AppointmentRepository.Overlap::id).toList();
    }

    private List<AppointmentRepository.Overlap> cruces(String inicio, int duracion, int defecto,
            Long excluida) {
        return repository.findOverlapping(EMPRESA, VETERINARIA, hora(inicio),
                hora(inicio).plusMinutes(duracion), defecto, excluida);
    }

    /**
     * La matriz que decide si la feature es usable. Con intervalos cerrados los dos
     * casos de borde darian conflicto y cada agenda encadenada quedaria bloqueada.
     */
    @Nested
    @DisplayName("Interseccion de intervalos semiabiertos")
    class Semiabierto {

        @ParameterizedTest(name = "existente {0} +{1}min  vs  nueva {2} +{3}min  ->  se cruzan: {4}")
        @CsvSource({
                // El borde exacto por la derecha: la nueva termina justo cuando la
                // existente empieza. Si esto diera true, la feature es inusable.
                "10:30, 30, 10:00, 30, false",
                // El borde exacto por la izquierda: la existente termina justo cuando la
                // nueva empieza.
                "09:30, 30, 10:00, 30, false", "10:15, 30, 10:00, 30, true",
                "10:00, 30, 10:00, 30, true", "10:15, 15, 10:00, 60, true",
                "11:00, 30, 10:00, 30, false",
                // Solapes por un solo minuto, a cada lado del borde.
                "10:29, 30, 10:00, 30, true", "09:31, 30, 10:00, 30, true"})
        @DisplayName("el cruce se decide con intervalos semiabiertos [inicio, fin)")
        void el_cruce_se_decide_con_intervalos_semiabiertos(String inicioExistente,
                int duracionExistente, String inicioNueva, int duracionNueva, boolean seCruzan) {
            Long existente = cita(inicioExistente, duracionExistente);

            List<Long> encontradas = solapes(inicioNueva, duracionNueva);

            assertThat(encontradas).isEqualTo(seCruzan ? List.of(existente) : List.of());
        }

        @Test
        @DisplayName("dos citas encadenadas no se estorban: 10:00-10:30 y 10:30-11:00 conviven")
        void dos_citas_encadenadas_no_se_estorban() {
            cita("10:00", 30);

            // El caso real que hundiria la agenda de cualquier clinica: consultas
            // seguidas, una detras de otra, sin un minuto de aire entre ellas.
            assertThat(solapes("10:30", 30)).isEmpty();
        }

        @Test
        @DisplayName("devuelve todas las citas cruzadas, no solo la primera")
        void devuelve_todas_las_citas_cruzadas() {
            Long primera = cita("09:45", 30);
            Long segunda = cita("10:15", 30);
            cita("11:30", 30);

            assertThat(solapes("10:00", 60)).containsExactlyInAnyOrder(primera, segunda);
        }
    }

    /**
     * Una cita con {@code duration_minutes} a NULL hereda la duracion por defecto
     * de la empresa. Es el estado de todas las citas anteriores a BE-17, asi que
     * este es el caso mayoritario en produccion el dia del despliegue.
     */
    @Nested
    @DisplayName("Duraciones mezcladas: NULL hereda el default de la empresa")
    class DuracionesMezcladas {

        @Test
        @DisplayName("una cita sin duracion propia se cruza o no segun el default de la empresa")
        void una_cita_sin_duracion_propia_depende_del_default() {
            Long existente = cita("09:45", null);

            // Con 30 minutos por defecto la existente ocupa 09:45-10:15 y pisa a la
            // nueva; con 10 ocupa 09:45-09:55 y le deja el hueco libre. Misma fila,
            // mismo intervalo nuevo, resultado opuesto: el default no es decorativo.
            assertThat(solapes("10:00", 30, 30, null)).containsExactly(existente);
            assertThat(solapes("10:00", 30, 10, null)).isEmpty();
        }

        @Test
        @DisplayName("la duracion explicita de la cita gana sobre el default de la empresa")
        void la_duracion_explicita_gana_sobre_el_default() {
            cita("09:45", 10);

            // Aunque la empresa diga 30, esta cita dura 10 y termina a las 09:55.
            assertThat(solapes("10:00", 30, 30, null)).isEmpty();
        }

        @Test
        @DisplayName("cruza una cita con duracion explicita contra otra que hereda el default")
        void cruza_duracion_explicita_contra_heredada() {
            Long heredada = cita("10:20", null);
            Long explicita = cita("09:50", 90);

            // heredada: 10:20-10:50 (default 30). explicita: 09:50-11:20. Nueva:
            // 10:00-10:30. Las tres se pisan.
            assertThat(solapes("10:00", 30)).containsExactlyInAnyOrder(heredada, explicita);
        }

        @Test
        @DisplayName("un default no positivo cae al respaldo de 30 en vez de anular el bloqueo")
        void un_default_no_positivo_cae_al_respaldo() {
            Long existente = cita("09:45", null);

            // El puerto acepta cualquier int; con un 0 la cita heredada duraria cero y
            // no se cruzaria con nada. El adaptador lo lleva al respaldo del dominio.
            assertThat(solapes("10:00", 30, 0, null)).containsExactly(existente);
        }
    }

    @Nested
    @DisplayName("Alcance de la consulta")
    class Alcance {

        @Test
        @DisplayName("no mira las citas de otra empresa")
        void no_mira_las_citas_de_otra_empresa() {
            cita("10:00", 30, AppointmentStatus.CONFIRMED, OTRA_EMPRESA, VETERINARIO_AJENO,
                    SEDE_AJENA);
            Long propia = cita("10:00", 30);

            assertThat(solapes("10:00", 30)).containsExactly(propia);
        }

        @Test
        @DisplayName("no mira las citas de otro veterinario de la misma empresa")
        void no_mira_las_citas_de_otro_veterinario() {
            cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA, OTRO_VETERINARIO, SEDE);

            // El alcance del cruce es el veterinario: dos vets pueden atender a la misma
            // hora en la misma clinica.
            assertThat(solapes("10:00", 30)).isEmpty();
        }

        @Test
        @DisplayName("el cruce ignora la sede: un vet no esta en dos consultas a la vez")
        void el_cruce_ignora_la_sede() {
            Long enOtraSede = cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA, VETERINARIA,
                    SchemaSeed.OTRA_BRANCH_ID);

            assertThat(solapes("10:00", 30)).containsExactly(enOtraSede);
        }

        @Test
        @DisplayName("cada cruce viaja con la sede en la que esta agendado")
        void cada_cruce_viaja_con_su_sede() {
            Long aqui = cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA, VETERINARIA, SEDE);
            Long alla = cita("10:10", 30, AppointmentStatus.CONFIRMED, EMPRESA, VETERINARIA,
                    SchemaSeed.OTRA_BRANCH_ID);

            // El cruce se calcula por veterinario, pero quien lo consume solo puede ver
            // las citas de sus sedes: sin este dato el 409 filtraba la agenda ajena.
            assertThat(cruces("10:00", 30, DEFECTO, null)).containsExactlyInAnyOrder(
                    new AppointmentRepository.Overlap(aqui, SEDE),
                    new AppointmentRepository.Overlap(alla, SchemaSeed.OTRA_BRANCH_ID));
        }

        @Test
        @DisplayName("una cita dada de baja no ocupa agenda")
        void una_cita_dada_de_baja_no_ocupa_agenda() {
            Long id = cita("10:00", 30);
            repository.delete(id, EMPRESA);
            entityManager.clear();

            // El @SQLRestriction("enabled = true") tiene que alcanzar tambien a esta
            // consulta; si no, una cita borrada seguiria bloqueando el hueco para siempre.
            assertThat(solapes("10:00", 30)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Estados que ocupan agenda")
    class Estados {

        @ParameterizedTest(name = "{0} no ocupa agenda")
        @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "NO_SHOW"})
        @DisplayName("una cita cancelada o no presentada libera el hueco")
        void una_cita_cancelada_o_no_presentada_libera_el_hueco(AppointmentStatus estado) {
            cita("10:00", 30, estado, EMPRESA, VETERINARIA, SEDE);

            assertThat(solapes("10:00", 30)).isEmpty();
        }

        @ParameterizedTest(name = "{0} si ocupa agenda")
        @EnumSource(value = AppointmentStatus.class, names = {"REQUESTED", "CONFIRMED", "ARRIVED",
                "IN_PROGRESS", "COMPLETED"})
        @DisplayName("el resto de estados si cuenta como choque, COMPLETED incluido")
        void el_resto_de_estados_si_cuenta_como_choque(AppointmentStatus estado) {
            Long id = cita("10:00", 30, estado, EMPRESA, VETERINARIA, SEDE);

            // COMPLETED es terminal pero ocupo el hueco del veterinario: agendar encima
            // de una consulta ya atendida sigue siendo un doble booking en la agenda.
            assertThat(solapes("10:00", 30)).containsExactly(id);
        }
    }

    @Nested
    @DisplayName("Exclusion de la propia cita")
    class Exclusion {

        @Test
        @DisplayName("la cita que se edita no choca consigo misma")
        void la_cita_que_se_edita_no_choca_consigo_misma() {
            Long propia = cita("10:00", 30);

            // Sin el excludeId ninguna edicion de una cita podria guardarse jamas: la
            // fila que esta en la base seria siempre su propio conflicto.
            assertThat(solapes("10:00", 30, DEFECTO, propia)).isEmpty();
        }

        @Test
        @DisplayName("excluir una cita no esconde las demas")
        void excluir_una_cita_no_esconde_las_demas() {
            Long propia = cita("10:00", 30);
            Long ajena = cita("10:15", 30);

            assertThat(solapes("10:00", 30, DEFECTO, propia)).containsExactly(ajena);
        }

        @Test
        @DisplayName("sin excludeId no se excluye nada: es el caso del alta")
        void sin_exclude_id_no_se_excluye_nada() {
            Long existente = cita("10:00", 30);

            assertThat(solapes("10:00", 30, DEFECTO, null)).containsExactly(existente);
        }
    }

    @Nested
    @DisplayName("Escritura y lectura de la cita")
    class Escritura {

        private Appointment nueva(Integer duracion) {
            return new Appointment(null, hora("10:00"), duracion, AppointmentType.CONSULTATION,
                    AppointmentStatus.REQUESTED, "Control anual", null, null, null, "Walk-in",
                    "3001234567", "walkin@example.com", new EmployeeRef(VETERINARIA, "Ana Ruiz"),
                    CompanyRef.of(EMPRESA), new BranchRef(SEDE, "Sede Centro", "CENTRO"), 0L, true,
                    CREADA);
        }

        @Test
        @DisplayName("guarda la duracion propia y la devuelve al releer")
        void guarda_la_duracion_propia_y_la_devuelve() {
            Appointment guardada = repository.save(nueva(45));
            entityManager.flush();
            entityManager.clear();

            assertThat(guardada.getId()).isNotNull();
            Appointment releida = repository.findByIdAndCompanyId(guardada.getId(), EMPRESA)
                    .orElseThrow();
            assertThat(releida.getDurationMinutes()).isEqualTo(45);
            assertThat(releida.getStartAt()).isEqualTo(hora("10:00"));
            assertThat(releida.endAt(DEFECTO)).isEqualTo(hora("10:45"));
        }

        @Test
        @DisplayName("una duracion nula viaja como NULL a la columna, no como cero")
        void una_duracion_nula_viaja_como_null() {
            Appointment guardada = repository.save(nueva(null));
            entityManager.flush();
            entityManager.clear();

            // La diferencia entre NULL y 0 es la que separa "hereda el ajuste de la
            // empresa" de "dura cero minutos", y solo se ve mirando la columna.
            Number columna = (Number) entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM appointments WHERE id = :id"
                            + " AND duration_minutes IS NULL")
                    .setParameter("id", guardada.getId()).getSingleResult();
            assertThat(columna.longValue()).isOne();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), EMPRESA).orElseThrow()
                    .getDurationMinutes()).isNull();
        }

        @Test
        @DisplayName("una cita de otra empresa no se lee: el filtro va en la consulta")
        void una_cita_de_otra_empresa_no_se_lee() {
            Appointment guardada = repository.save(nueva(30));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), OTRA_EMPRESA)).isEmpty();
        }

        @Test
        @DisplayName("la cita recien guardada aparece en el listado filtrado del dia")
        void la_cita_guardada_aparece_en_el_listado_del_dia() {
            Appointment guardada = repository.save(nueva(45));
            entityManager.flush();
            entityManager.clear();

            List<Appointment> delDia = repository.findByFilters(EMPRESA, hora("00:00"),
                    hora("23:59"), VETERINARIA, AppointmentStatus.REQUESTED, SEDE);

            assertThat(delDia).extracting(Appointment::getId).containsExactly(guardada.getId());
            assertThat(delDia).first().extracting(Appointment::getDurationMinutes).isEqualTo(45);
        }
    }

    /**
     * Issue #114, changeset 226. La ultima linea de defensa del solape EXACTO: dos
     * transacciones que pasan las dos por {@code findOverlapping} sin verse no
     * pueden acabar las dos insertadas. Como MySQL no tiene indices unicos
     * filtrados, el alcance se emula con la columna generada
     * {@code active_slot_employee_id}, que vale NULL —y por tanto no reserva nada—
     * cuando la cita no ocupa agenda.
     *
     * <p>
     * <b>Lo que estos casos protegen es esa columna generada, no el indice.</b> Un
     * indice unico sobre {@code (employee_id, start_at)} a secas se lee igual de
     * bien en el diff y rechazaria con un 409 la cita nueva que ocupa el hueco de
     * una CANCELADA — es decir, dejaria de poder reagendarse una cita anulada, que
     * es la operacion mas normal de una recepcion. Esa diferencia solo se ve contra
     * una base real.
     */
    @Nested
    @DisplayName("Slot unico de la cita activa (uq_appointments_active_employee_start)")
    class SlotUnico {

        private void darDeBaja(Long id) {
            entityManager
                    .createNativeQuery("UPDATE appointments SET enabled = false WHERE id = :id")
                    .setParameter("id", id).executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("dos citas activas del mismo veterinario a la misma hora no caben las dos")
        void dos_citas_activas_a_la_misma_hora_no_caben() {
            cita("10:00", 30);

            assertThatThrownBy(() -> cita("10:00", 30))
                    .hasStackTraceContaining("uq_appointments_active_employee_start");
        }

        @ParameterizedTest(name = "{0}")
        @DisplayName("una cita que no ocupa agenda deja el hueco libre: se puede reagendar encima")
        @EnumSource(value = AppointmentStatus.class, names = {"CANCELLED", "NO_SHOW"})
        void una_cita_que_no_ocupa_agenda_deja_el_hueco_libre(AppointmentStatus estado) {
            cita("10:00", 30, estado, EMPRESA, VETERINARIA, SEDE);

            Long reagendada = cita("10:00", 30);

            assertThat(filas("appointments", reagendada)).as("la cita que ocupa el hueco liberado")
                    .isOne();
        }

        @Test
        @DisplayName("una cita dada de baja tampoco reserva el hueco")
        void una_cita_dada_de_baja_tampoco_reserva_el_hueco() {
            darDeBaja(cita("10:00", 30));

            Long nueva = cita("10:00", 30);

            assertThat(filas("appointments", nueva)).isOne();
        }

        @Test
        @DisplayName("cancelar una cita libera su hueco en el acto, sin borrarla")
        void cancelar_una_cita_libera_su_hueco_en_el_acto() {
            Long ocupada = cita("10:00", 30);
            entityManager
                    .createNativeQuery(
                            "UPDATE appointments SET status = 'CANCELLED' WHERE id = :id")
                    .setParameter("id", ocupada).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            // La columna es STORED: si no se recalculara en el UPDATE, el hueco quedaria
            // reservado por una cita cancelada y nadie podria reagendar esa hora.
            assertThatCode(() -> cita("10:00", 30)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("el alcance es por veterinario: otro profesional a la misma hora si cabe")
        void otro_veterinario_a_la_misma_hora_si_cabe() {
            cita("10:00", 30);

            Long delOtro = cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA, OTRO_VETERINARIO,
                    SEDE);

            assertThat(filas("appointments", delOtro)).isOne();
        }

        @Test
        @DisplayName("la misma hora en otro minuto no la ve la constraint: ese caso lo cubre el lock")
        void la_misma_hora_en_otro_minuto_no_la_ve_la_constraint() {
            cita("10:00", 30);

            // 10:15 se pisa con 10:00-10:30 pero tiene otro start_at, asi que el indice
            // no la toca. Esta es la mitad del problema que sostiene
            // CreateAppointmentService.lockForOverlapCheck, y dejarlo escrito aqui evita
            // que alguien lea el changeset 226 como "el solape ya esta cerrado en la BD".
            Long parcial = cita("10:15", 30);

            assertThat(filas("appointments", parcial)).isOne();
            assertThat(solapes("10:15", 30)).isNotEmpty();
        }
    }

    /**
     * Issue #240. La regresion que nadie veia: el indice del #114 no distingue una
     * carrera accidental de un doble booking deliberado, asi que forzar dos citas a
     * la misma hora —la urgencia que la recepcion encaja a diario— moria contra la
     * constraint y salia por la web como un 409 con el texto de la carrera.
     *
     * <p>
     * <b>Por que estos casos van aqui y no arriba con los mocks.</b> Los tres tests
     * de forzado que ya existian ({@code CreateAppointmentServiceTest},
     * {@code UpdateAppointmentServiceTest},
     * {@code RescheduleAppointmentServiceTest}) doblan el repositorio: su
     * {@code save} devuelve el argumento y nunca toca una base, asi que daban verde
     * con el defecto vivo delante. La unica prueba que puede ver esto es una que
     * llegue al INSERT real.
     *
     * <p>
     * El mecanismo es {@code overlap_forced}: una cita forzada renuncia a reservar
     * su hueco —su {@code active_slot_employee_id} vale NULL— y por eso convive con
     * la que ya lo ocupaba. Lo que NO cambia es la carrera: dos citas normales a la
     * misma hora se siguen rechazando, y el ultimo caso de esta clase esta aqui
     * justamente para que nadie "arregle" el #240 borrando el indice.
     */
    @Nested
    @DisplayName("Doble booking deliberado (issue #240)")
    class SlotForzado {

        /** Cita de dominio lista para guardar por el camino de escritura real. */
        private Appointment nueva(String inicio, boolean forzada) {
            Appointment cita = new Appointment(null, hora(inicio), 30, AppointmentType.CONSULTATION,
                    AppointmentStatus.REQUESTED, "Urgencia", null, null, null, "Walk-in",
                    "3001234567", null, new EmployeeRef(VETERINARIA, "Ana Ruiz"),
                    CompanyRef.of(EMPRESA), new BranchRef(SEDE, "Sede Centro", "CENTRO"), 0L, true,
                    CREADA);
            cita.markOverlapForced(forzada);
            return cita;
        }

        @Test
        @DisplayName("una cita forzada cabe encima de la que ya ocupaba el hueco")
        void una_cita_forzada_cabe_encima_de_la_que_ocupaba_el_hueco() {
            cita("10:00", 30);

            // ESTE es el defecto del #240. Antes de persistir la decision, este INSERT
            // violaba uq_appointments_active_employee_start y el usuario recibia un 409
            // diciendole que el veterinario "acaba de quedar ocupado" — un segundo
            // despues de haber pedido explicitamente forzar ese mismo hueco.
            Long forzada = cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA, VETERINARIA,
                    SEDE, true);

            assertThat(filas("appointments", forzada)).isOne();
        }

        @Test
        @DisplayName("varias citas forzadas conviven en el mismo hueco, no solo dos")
        void varias_citas_forzadas_conviven_en_el_mismo_hueco() {
            cita("10:00", 30);
            Long primeraForzada = cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA,
                    VETERINARIA, SEDE, true);

            Long segundaForzada = cita("10:00", 30, AppointmentStatus.CONFIRMED, EMPRESA,
                    VETERINARIA, SEDE, true);

            // Si el arreglo hubiera sido meter overlap_forced DENTRO de la clave unica
            // en vez de anular el marcador, cabrian exactamente dos citas —una normal y
            // una forzada— y la tercera volveria a dar 409. La agenda de una urgencia no
            // tiene ese tope.
            assertThat(filas("appointments", primeraForzada)).isOne();
            assertThat(filas("appointments", segundaForzada)).isOne();
        }

        @Test
        @DisplayName("forzar no reabre la carrera: dos citas normales siguen sin caber")
        void forzar_no_reabre_la_carrera() {
            cita("10:00", 30);

            // La red de seguridad del #114 sigue puesta. Sin este caso, el #240 se
            // "arregla" borrando el indice y las dos peticiones concurrentes que nadie
            // forzo vuelven a guardarse las dos, con sus dos correos de confirmacion.
            assertThatThrownBy(() -> cita("10:00", 30))
                    .hasStackTraceContaining("uq_appointments_active_employee_start");
        }

        @Test
        @DisplayName("la cita forzada no reserva su hueco; la normal si lo reserva")
        void la_cita_forzada_no_reserva_su_hueco() {
            Long normal = cita("11:00", 30);
            Long forzada = cita("11:00", 30, AppointmentStatus.CONFIRMED, EMPRESA, VETERINARIA,
                    SEDE, true);

            // El mecanismo, dicho sin rodeos: quien arbitra el hueco de una cita forzada
            // ya no es la base, es el check de findOverlapping del service. La base solo
            // arbitra lo que nadie decidio a mano.
            assertThat(citasQue("active_slot_employee_id IS NOT NULL", normal))
                    .as("la cita normal reserva el hueco").isOne();
            assertThat(citasQue("active_slot_employee_id IS NULL", forzada))
                    .as("la cita forzada renuncia a reservarlo").isOne();
            // Y el cruce se sigue viendo: la forzada NO desaparece de la agenda, asi que
            // una tercera cita sin forzar se topa con las dos y el service la bloqueara.
            assertThat(solapes("11:00", 30)).containsExactlyInAnyOrder(normal, forzada);
        }

        @Test
        @DisplayName("el camino de escritura real persiste el forzado y lo devuelve al releer")
        void el_camino_de_escritura_persiste_el_forzado() {
            repository.save(nueva("12:00", false));
            entityManager.flush();
            entityManager.clear();

            Appointment guardada = repository.save(nueva("12:00", true));
            entityManager.flush();
            entityManager.clear();

            // La mitad del arreglo vive en el mapper: si toJpa no bajara el flag, este
            // save reventaria igual que antes aunque el dominio lo supiera.
            assertThat(citasQue("overlap_forced = TRUE", guardada.getId())).isOne();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), EMPRESA).orElseThrow()
                    .isOverlapForced()).isTrue();
        }

        @Test
        @DisplayName("reescribir una cita forzada no la hace pelear por el hueco otra vez")
        void reescribir_una_cita_forzada_no_la_hace_pelear_por_el_hueco() {
            repository.save(nueva("13:00", false));
            entityManager.flush();
            entityManager.clear();
            Long forzada = repository.save(nueva("13:00", true)).getId();
            entityManager.flush();
            entityManager.clear();

            // Cualquier escritura posterior sobre esa cita —cambiarle el estado,
            // cancelarla— la lee y la vuelve a guardar. Si el flag no viajara de vuelta
            // en toDomain, el UPDATE recalcularia el marcador, chocaria con la cita
            // normal de las 13:00 y una cita forzada quedaria imposible de tocar.
            Appointment releida = repository.findByIdAndCompanyId(forzada, EMPRESA).orElseThrow();
            releida.transitionTo(AppointmentStatus.CONFIRMED);

            assertThatCode(() -> {
                repository.save(releida);
                entityManager.flush();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("sacar una cita de un hueco compartido le devuelve la reserva")
        void sacar_una_cita_de_un_hueco_compartido_le_devuelve_la_reserva() {
            repository.save(nueva("14:00", false));
            entityManager.flush();
            entityManager.clear();
            Long forzada = repository.save(nueva("14:00", true)).getId();
            entityManager.flush();
            entityManager.clear();

            // La reprogramacion la mueve a un hueco libre y ya no fuerza nada, asi que
            // vuelve a reservar: si el flag se quedara pegado a true, esa cita quedaria
            // exenta del indice para siempre y su hueco nuevo si admitiria la carrera.
            Appointment releida = repository.findByIdAndCompanyId(forzada, EMPRESA).orElseThrow();
            releida.reschedule(hora("15:00"), 30, new EmployeeRef(VETERINARIA, "Ana Ruiz"));
            releida.markOverlapForced(false);
            repository.save(releida);
            entityManager.flush();
            entityManager.clear();

            assertThat(citasQue("active_slot_employee_id IS NOT NULL", forzada)).isOne();
            assertThatThrownBy(() -> cita("15:00", 30))
                    .hasStackTraceContaining("uq_appointments_active_employee_start");
        }
    }

    /**
     * Issue #241. Que parte del solape frena la base cuando la cita se MUEVE, y
     * cual no. El lock pesimista por veterinario existe justamente para lo que aqui
     * queda sin frenar: hasta este arreglo lo tomaba solo el alta, y editar y
     * reprogramar hacian el mismo leer-y-escribir a pelo.
     *
     * <p>
     * <b>Que prueban estos casos y que no.</b> Prueban, contra MySQL real y por el
     * camino de escritura de verdad, que el UPDATE de una cita no esta protegido
     * por {@code uq_appointments_active_employee_start} salvo en el caso exacto y
     * contra una cita que no este forzada — o sea, que el arbitro del cruce en
     * estos dos verbos es el {@code findOverlapping} del servicio y nada mas. NO
     * prueban el bloqueo: eso exige dos transacciones a la vez y este arnes es
     * {@code @DataJpaTest}, una transaccion por prueba que ademas revierte al
     * final, asi que la siembra ni siquiera es visible desde otra conexion. La
     * carrera con dos hilos reales sigue pendiente en el issue #225 y cubrira los
     * tres verbos de golpe. El orden de las sentencias —el lock ANTES de leer— lo
     * fijan {@code UpdateAppointmentServiceTest} y
     * {@code RescheduleAppointmentServiceTest}.
     */
    @Nested
    @DisplayName("Lo que la base NO arbitra al mover una cita (issue #241)")
    class MoverLaCita {

        /**
         * Siembra una cita por el <b>camino de escritura real</b> y devuelve su id.
         *
         * <p>
         * <b>Por que no sirve aqui el helper nativo {@code cita(...)} de la clase
         * externa.</b> Ese inserta por SQL directo y deja la fila sin animal, sin dueno
         * y sin nombre de cliente, y el agregado exige al menos uno de los tres
         * ({@code Appointment.validate}). Da igual mientras la fila solo se
         * <i>consulte</i> —{@code findOverlapping} devuelve proyecciones
         * {@code Overlap(id, sede)}, y {@code filas} y {@code citasQue} son COUNT—, que
         * es todo lo que hacian {@link Semiabierto}, {@link Alcance} o
         * {@link SlotUnico}. En cuanto la fila se <i>relee como agregado</i> por
         * {@code findByIdAndCompanyId}, el mapper la reconstruye y el constructor la
         * rechaza. Los casos de esta clase son los primeros del fichero que releen lo
         * que sembraron, asi que siembran por el mismo camino por el que van a leer —
         * igual que {@link SlotForzado}, que ya lo hacia por el mismo motivo.
         */
        private Long sembrar(String inicio, boolean forzada) {
            Appointment cita = new Appointment(null, hora(inicio), 30, AppointmentType.CONSULTATION,
                    AppointmentStatus.REQUESTED, "Sembrada", null, null, null, "Walk-in",
                    "3001234567", null, new EmployeeRef(VETERINARIA, "Ana Ruiz"),
                    CompanyRef.of(EMPRESA), new BranchRef(SEDE, "Sede Centro", "CENTRO"), 0L, true,
                    CREADA);
            cita.markOverlapForced(forzada);
            Long id = repository.save(cita).getId();
            entityManager.flush();
            entityManager.clear();
            return id;
        }

        /**
         * Mueve la cita por el camino de escritura real —leer, reprogramar, guardar—,
         * que es lo que hacen {@code UpdateAppointmentService} y
         * {@code RescheduleAppointmentService} despues de decidir el cruce.
         */
        private void moverA(Long id, String inicio) {
            Appointment cita = repository.findByIdAndCompanyId(id, EMPRESA).orElseThrow();
            cita.reschedule(hora(inicio), 30, new EmployeeRef(VETERINARIA, "Ana Ruiz"));
            repository.save(cita);
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("mover una cita a un minuto distinto sobre un hueco ocupado no lo frena la base")
        void mover_a_un_minuto_distinto_no_lo_frena_la_base() {
            Long ocupada = sembrar("10:00", false);
            Long movida = sembrar("09:00", false);

            // 10:15 se pisa con 10:00-10:30 pero tiene otro start_at, asi que el marcador
            // generado no colisiona y el UPDATE pasa limpio. Este es el agujero entero
            // del #241: si el servicio no serializa, dos reprogramaciones concurrentes
            // llegan las dos hasta aqui y la base no dice ni una palabra.
            assertThatCode(() -> moverA(movida, "10:15")).doesNotThrowAnyException();

            assertThat(solapes("10:15", 30)).as("las dos citas se pisan en la agenda")
                    .containsExactlyInAnyOrder(ocupada, movida);
        }

        @Test
        @DisplayName("mover una cita a la hora exacta de otra si lo frena la base")
        void mover_a_la_hora_exacta_si_lo_frena_la_base() {
            sembrar("10:00", false);
            Long movida = sembrar("09:00", false);

            // La otra mitad, la que si esta cubierta: la columna generada es STORED y se
            // recalcula en el UPDATE, no solo en el INSERT. Sin este caso nadie sabria
            // que el indice tambien vigila el camino de la edicion.
            assertThatThrownBy(() -> moverA(movida, "10:00"))
                    .hasStackTraceContaining("uq_appointments_active_employee_start");
        }

        @Test
        @DisplayName("mover una cita encima de una forzada no lo frena nada, ni en la hora exacta")
        void mover_encima_de_una_forzada_no_lo_frena_nada() {
            Long forzada = sembrar("10:00", true);
            Long movida = sembrar("09:00", false);

            // La consecuencia del #240 que el #241 hereda: la cita forzada dejo su
            // active_slot_employee_id a NULL, asi que ya no reserva su hueco y hasta el
            // solape EXACTO pasa por debajo del indice. Quien tiene que rechazar esta
            // escritura —o pedir que se fuerce— es el findOverlapping del servicio, y
            // por eso ese leer-y-escribir tiene que ir serializado.
            assertThatCode(() -> moverA(movida, "10:00")).doesNotThrowAnyException();

            assertThat(solapes("10:00", 30)).as("el cruce existe y solo el servicio lo ve")
                    .containsExactlyInAnyOrder(forzada, movida);
        }
    }
}
