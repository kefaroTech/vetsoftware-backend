package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
        Long id = siguienteId++;
        entityManager.createNativeQuery("""
                INSERT INTO appointments (id, start_at, duration_minutes, type, status, notes,
                                          employee_id, company_id, branch_id, version, enabled,
                                          created_date)
                VALUES (:id, :inicio, :duracion, 'CONSULTATION', :estado, 'Sembrada', :empleado,
                        :empresa, :sede, 0, true, :creada)
                """).setParameter("id", id).setParameter("inicio", hora(inicio))
                .setParameter("duracion", duracion).setParameter("estado", estado.name())
                .setParameter("empleado", empleado).setParameter("empresa", empresa)
                .setParameter("sede", sede).setParameter("creada", CREADA).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return id;
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
}
