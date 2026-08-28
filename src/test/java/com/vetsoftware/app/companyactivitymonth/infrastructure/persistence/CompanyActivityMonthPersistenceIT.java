package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companyactivitymonth.domain.ActivityPeriodKey;
import com.vetsoftware.app.companyactivitymonth.domain.CommercialState;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonthAlreadyExistsException;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCompanyActivityMonthRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las dos cosas que no se pueden
 * comprobar sin el motor</b>, y las dos deciden si un informe de actividad dice
 * la verdad:
 *
 * <ol>
 * <li><b>Que {@code uq_cam_month} llegue traducido.</b> El adaptador escribe
 * con {@code saveAndFlush} y no con {@code save} justamente para que el
 * duplicado choque <em>dentro</em> de su {@code try}. Con {@code save} la
 * escritura se queda encolada, la violacion salta al hacer commit —fuera del
 * metodo y fuera del {@code catch}— y al cliente le llega un error de
 * integridad crudo en vez del 409 que dice que el mes ya existe. Un caso que
 * hiciera el {@code flush()} a mano no probaria nada de esto: pasaria igual con
 * las dos implementaciones.</li>
 * <li><b>Que el recalculo sea una edicion y no un insert.</b> Depende de que el
 * mapper copie la version en los dos sentidos; si la perdiera, Hibernate
 * tomaria la entidad por transitoria y escribiria una fila nueva que ademas
 * chocaria contra {@code uq_cam_month} — una violacion de unicidad en una
 * operacion que no insertaba nada.</li>
 * </ol>
 *
 * <p>
 * <b>Y fija que el dominio y el motor dicen ya LO MISMO sobre el
 * calendario.</b> Hubo un tiempo en que no: {@code chk_cam_active_days} (355)
 * solo llegaba a {@code BETWEEN 0 AND 31}, asi que el motor aceptaba 30 dias
 * activos en un febrero de 28 y quien lo rechazaba era solo
 * {@link ActivityPeriodKey#lengthOfMonth()}. El changeset <b>376</b> cerro esa
 * rendija con {@code chk_cam_active_days_calendar}, un CHECK que cuenta los
 * dias del mes mirando unicamente columnas de la propia fila. Los casos
 * {@code treinta_dias_en_febrero_los_paran_los_dos} y
 * {@code veintiocho_dias_en_febrero_si_entran} afirman las dos mitades y su
 * borde exacto.
 *
 * <p>
 * <b>Los meses van todos en 2029</b> para no cruzarse con ninguna siembra ni
 * con las claves de otras rodajas, y las empresas son las dos de
 * {@link SchemaSeed}: {@code fk_cam_company} es {@code RESTRICT} y sin una
 * empresa real no se puede escribir un solo mes.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de todas las rodajas
 * para que compartan una unica clave de {@code MergedContextConfiguration} y,
 * con ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio
 * con este adaptador volveria a darle a esta clase una clave unica y un
 * arranque de contexto entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyActivityMonthRepository — la serie de actividad contra MySQL real")
class CompanyActivityMonthPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a las escrituras crudas de esta rodaja. */
    private static final Long FILA_CRUDA = 8800L;

    private static final ActivityPeriodKey ENERO = new ActivityPeriodKey("2029-01");
    private static final ActivityPeriodKey FEBRERO = new ActivityPeriodKey("2029-02");
    private static final ActivityPeriodKey MARZO = new ActivityPeriodKey("2029-03");

    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2029, 2, 1, 3, 15, 0);

    @Autowired
    private CompanyActivityMonthJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaCompanyActivityMonthRepository repository;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        repository = new JpaCompanyActivityMonthRepository(springDataRepository,
                new CompanyActivityMonthJpaMapper());
    }

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("guarda el mes y lo recupera con los cinco numeros en su sitio")
        void guarda_el_mes_y_lo_recupera_campo_a_campo() {
            // Los tres contadores llevan valores distintos a proposito: son tres int
            // consecutivos en el constructor y cruzarlos compila sin una queja.
            CompanyActivityMonth guardado = repository.save(mes(SchemaSeed.COMPANY_ID, ENERO,
                    CommercialState.PAID, 21, 7, 143, "189000.00"));
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(recuperado.getPeriodKey()).isEqualTo(ENERO);
                assertThat(recuperado.getCommercialState()).isEqualTo(CommercialState.PAID);
                assertThat(recuperado.getActiveDays()).isEqualTo(21);
                assertThat(recuperado.getActiveUsers()).isEqualTo(7);
                assertThat(recuperado.getRecordsCreated()).isEqualTo(143);
                assertThat(recuperado.getMrrSnapshot()).isEqualByComparingTo("189000.00");
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperado.getVersion()).isZero();
                assertThat(recuperado.isPaid()).isTrue();
            });
        }

        @Test
        @DisplayName("dos clinicas distintas caben en el mismo mes: la unicidad es del par")
        void dos_clinicas_distintas_caben_en_el_mismo_mes() {
            repository.save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 21, 7, 143,
                    "189000.00"));
            repository.save(
                    mes(SchemaSeed.OTRA_COMPANY_ID, ENERO, CommercialState.TRIAL, 3, 1, 4, "0.00"));
            entityManager.clear();

            assertThat(repository.findAllByPeriodKey(ENERO.value(), 0, 20).content())
                    .extracting(CompanyActivityMonth::getCompanyId)
                    .containsExactlyInAnyOrder(SchemaSeed.COMPANY_ID, SchemaSeed.OTRA_COMPANY_ID);
        }

        @Test
        @DisplayName("la misma clinica en dos meses distintos cabe, y sale lo mas reciente primero")
        void la_misma_clinica_en_dos_meses_distintos_cabe() {
            repository.save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 21, 7, 143,
                    "189000.00"));
            repository.save(mes(SchemaSeed.COMPANY_ID, FEBRERO, CommercialState.PAID, 18, 6, 120,
                    "189000.00"));
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(month -> month.getPeriodKey().value())
                    .containsExactly(FEBRERO.value(), ENERO.value());
        }

        @Test
        @DisplayName("una empresa que no existe la para fk_cam_company")
        void una_empresa_inexistente_la_para_la_clave_foranea() {
            // RESTRICT sobre companies: sin este cinturon, un mes podria quedar colgando
            // de una clinica borrada y el informe de actividad no tendria a quien
            // atribuirlo.
            EngineConstraint.assertViolates("fk_cam_company",
                    () -> insertarCruda(FILA_CRUDA, 999999L, "2029-04", "PAID", 5, 1, 1, "0.00"));
        }

        @Test
        @DisplayName("el mismo par clinica-mes dos veces sale traducido, no como error crudo")
        void el_mismo_par_dos_veces_sale_traducido() {
            // ESTE CASO ES LA PRUEBA DE QUE EL ADAPTADOR USA saveAndFlush Y NO save.
            //
            // No hay ningun entityManager.flush() aqui, y esa ausencia es el punto: con
            // save la escritura se quedaria encolada, no chocaria dentro del try del
            // adaptador y este metodo NO lanzaria nada —la violacion saldria despues, al
            // commit, ya fuera del catch, y llegaria al cliente como un error de
            // integridad crudo en vez del 409 que sabe leer—.
            //
            // Va el ultimo del bloque a proposito: la violacion marca la transaccion
            // como rollback-only y cualquier escritura posterior fallaria por arrastre.
            repository.save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 21, 7, 143,
                    "189000.00"));

            assertThatThrownBy(() -> repository
                    .save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.FREE, 2, 1, 0, "0.00")))
                    .isInstanceOf(CompanyActivityMonthAlreadyExistsException.class)
                    .hasMessageContaining(ENERO.value());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un periodo trimestral lo para el motor: esta tabla es mensual")
        void un_periodo_trimestral_lo_para_el_motor() {
            // La diferencia deliberada con company_usage_events, que si admite 2029-Q1 y
            // ALLTIME. Aqui una clave trimestral partiria la serie en dos granularidades
            // y el listado dejaria de ordenar como el calendario.
            // OJO, Y ESTO ES UN HALLAZGO, NO UN AJUSTE COSMETICO.
            //
            // Este caso nacio afirmando chk_cam_period_key. Desde el changeset 376
            // existe chk_cam_active_days_calendar, que calcula los dias del mes con
            // DAY(LAST_DAY(STR_TO_DATE(CONCAT(period_key,'-01'),'%Y-%m-%d'))). Con una
            // clave que no es AAAA-MM ese STR_TO_DATE no produce una fecha util, y el
            // motor da por violada la restriccion del calendario ANTES de llegar a la
            // de la clave. Se probo con active_days = 0, que satisface cualquier techo:
            // sigue saltando la del calendario.
            //
            // O sea que UNA RESTRICCION NUEVA VOLVIO INOBSERVABLE A UNA VIEJA por esta
            // via. La fila se rechaza igual —que es lo que protege la tabla— pero ya no
            // se puede demostrar desde aqui CUAL de las dos la rechaza. Se afirma la que
            // de verdad para la sentencia, en vez de dejar el caso rojo o, peor,
            // relajarlo hasta que pase sin decir nada.
            EngineConstraint.assertViolates("chk_cam_active_days_calendar",
                    () -> insertarCruda(FILA_CRUDA + 1, SchemaSeed.COMPANY_ID, "2029-Q1", "PAID", 0,
                            1, 1, "0.00"));
        }

        @Test
        @DisplayName("un mes 13 lo para el motor")
        void un_mes_13_lo_para_el_motor() {
            // El dominio ya rechaza esta clave, asi que la unica forma de comprobar que
            // la base tambien la rechaza —el cinturon bajo el tirante— es escribir la
            // fila por SQL nativo, saltandose el value object.
            // OJO, Y ESTO ES UN HALLAZGO, NO UN AJUSTE COSMETICO.
            //
            // Este caso nacio afirmando chk_cam_period_key. Desde el changeset 376
            // existe chk_cam_active_days_calendar, que calcula los dias del mes con
            // DAY(LAST_DAY(STR_TO_DATE(CONCAT(period_key,'-01'),'%Y-%m-%d'))). Con una
            // clave que no es AAAA-MM ese STR_TO_DATE no produce una fecha util, y el
            // motor da por violada la restriccion del calendario ANTES de llegar a la
            // de la clave. Se probo con active_days = 0, que satisface cualquier techo:
            // sigue saltando la del calendario.
            //
            // O sea que UNA RESTRICCION NUEVA VOLVIO INOBSERVABLE A UNA VIEJA por esta
            // via. La fila se rechaza igual —que es lo que protege la tabla— pero ya no
            // se puede demostrar desde aqui CUAL de las dos la rechaza. Se afirma la que
            // de verdad para la sentencia, en vez de dejar el caso rojo o, peor,
            // relajarlo hasta que pase sin decir nada.
            EngineConstraint.assertViolates("chk_cam_active_days_calendar",
                    () -> insertarCruda(FILA_CRUDA + 2, SchemaSeed.COMPANY_ID, "2029-13", "PAID", 0,
                            1, 1, "0.00"));
        }

        @Test
        @DisplayName("un estado comercial fuera de los cuatro lo para chk_cam_state")
        void un_estado_comercial_desconocido_lo_para_el_motor() {
            // La red del enum: si alguien anade un quinto valor a CommercialState sin el
            // changeset que lo admita, el INSERT muere aqui y no en produccion.
            EngineConstraint.assertViolates("chk_cam_state", () -> insertarCruda(FILA_CRUDA + 3,
                    SchemaSeed.COMPANY_ID, "2029-04", "PENDING", 5, 1, 1, "0.00"));
        }

        @Test
        @DisplayName("treinta y dos dias activos los para chk_cam_active_days")
        void treinta_y_dos_dias_activos_los_para_el_motor() {
            EngineConstraint.assertViolates("chk_cam_active_days",
                    () -> insertarCruda(FILA_CRUDA + 4, SchemaSeed.COMPANY_ID, "2029-04", "PAID",
                            32, 1, 1, "0.00"));
        }

        @Test
        @DisplayName("usuarios activos negativos los para chk_cam_active_users")
        void usuarios_activos_negativos_los_para_el_motor() {
            EngineConstraint.assertViolates("chk_cam_active_users",
                    () -> insertarCruda(FILA_CRUDA + 5, SchemaSeed.COMPANY_ID, "2029-04", "PAID", 5,
                            -1, 1, "0.00"));
        }

        @Test
        @DisplayName("registros creados negativos los para chk_cam_records")
        void registros_creados_negativos_los_para_el_motor() {
            EngineConstraint.assertViolates("chk_cam_records", () -> insertarCruda(FILA_CRUDA + 6,
                    SchemaSeed.COMPANY_ID, "2029-04", "PAID", 5, 1, -1, "0.00"));
        }

        @Test
        @DisplayName("un MRR negativo lo para chk_cam_mrr")
        void un_mrr_negativo_lo_para_el_motor() {
            // Cero es legitimo —un mes FREE, TRIAL o CHURNED no factura—, negativo no:
            // seria un ingreso recurrente que resta, que no significa nada.
            EngineConstraint.assertViolates("chk_cam_mrr", () -> insertarCruda(FILA_CRUDA + 7,
                    SchemaSeed.COMPANY_ID, "2029-04", "PAID", 5, 1, 1, "-0.01"));
        }

        @Test
        @DisplayName("treinta dias en febrero los paran LOS DOS: el dominio y el motor")
        void treinta_dias_en_febrero_los_paran_los_dos() {
            // ESTE CASO ESTUVO ESCRITO AL REVES Y HAY QUE SABER POR QUE.
            //
            // Nacio afirmando una ASIMETRIA: que el dominio rechazaba 30 dias en un
            // febrero de 28 pero el motor los aceptaba, porque chk_cam_active_days (355)
            // solo llega a BETWEEN 0 AND 31 y un CHECK no puede consultar un calendario.
            // Eso era cierto cuando se escribio.
            //
            // El changeset 376 la cerro con un CHECK que SI sabe contar el mes, sin
            // disparador y sin subconsulta, mirando solo columnas de la propia fila:
            // active_days <=
            // DAY(LAST_DAY(STR_TO_DATE(CONCAT(period_key,'-01'),'%Y-%m-%d')))
            // Desde entonces el motor tambien la para, y el caso viejo llevaba rojo
            // porque seguia esperando que la fila entrara.
            //
            // Las dos mitades se afirman juntas a proposito: el cinturon y el tirante.
            // Si alguien retirase 376 pensando que el dominio ya basta, la segunda mitad
            // se pondria roja y diria exactamente que se perdio.
            assertThatThrownBy(() -> mes(SchemaSeed.COMPANY_ID, FEBRERO, CommercialState.PAID, 30,
                    1, 1, "0.00")).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("28 days of 2029-02");

            EngineConstraint.assertViolates("chk_cam_active_days_calendar",
                    () -> insertarCruda(FILA_CRUDA + 8, SchemaSeed.COMPANY_ID, FEBRERO.value(),
                            "PAID", 30, 1, 1, "0.00"));
        }

        @Test
        @DisplayName("veintiocho dias en ese mismo febrero si entran: el techo es el del mes")
        void veintiocho_dias_en_febrero_si_entran() {
            // El gemelo en verde, y sin el la pareja no vale: un CHECK que rechazara
            // TODO tambien pondria verde el caso de arriba. Veintiocho es el limite
            // exacto de 2029-02, asi que este caso fija el borde por el lado bueno.
            insertarCruda(FILA_CRUDA + 9, SchemaSeed.COMPANY_ID, FEBRERO.value(), "PAID", 28, 1, 1,
                    "0.00");
            entityManager.flush();
            entityManager.clear();

            assertThat(
                    repository.findByCompanyIdAndPeriodKey(SchemaSeed.COMPANY_ID, FEBRERO.value()))
                    .get().satisfies(fila -> assertThat(fila.getActiveDays()).isEqualTo(28));
        }
    }

    @Nested
    @DisplayName("Recalculo")
    class Recalculo {

        @Test
        @DisplayName("el recalculo reescribe los cinco numeros y mueve la version")
        void el_recalculo_reescribe_los_numeros_y_mueve_la_version() {
            // La forma de escritura que define esta tabla: la fila del mes en curso se
            // recalcula sobre si misma cada dia hasta que el mes termina.
            CompanyActivityMonth guardado = repository.save(
                    mes(SchemaSeed.COMPANY_ID, MARZO, CommercialState.TRIAL, 3, 1, 8, "0.00"));
            entityManager.clear();

            CompanyActivityMonth cargado = repository.findById(guardado.getId()).orElseThrow();
            repository.save(cargado.recalculate(CommercialState.PAID, 19, 6, 211,
                    new BigDecimal("189000.00")));
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recalculado -> {
                assertThat(recalculado.getCommercialState()).isEqualTo(CommercialState.PAID);
                assertThat(recalculado.getActiveDays()).isEqualTo(19);
                assertThat(recalculado.getActiveUsers()).isEqualTo(6);
                assertThat(recalculado.getRecordsCreated()).isEqualTo(211);
                assertThat(recalculado.getMrrSnapshot()).isEqualByComparingTo("189000.00");
                // La version se movio: el UPDATE paso por el ciclo de Hibernate y no por
                // una escritura masiva, que la habria dejado intacta (#53).
                assertThat(recalculado.getVersion()).isEqualTo(1L);
                // Lo que el recalculo NO puede mover: llevarse la actividad de una
                // clinica a otra, o la de marzo a otro mes, no es recalcular sino
                // reescribir la historia.
                assertThat(recalculado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(recalculado.getPeriodKey()).isEqualTo(MARZO);
                assertThat(recalculado.getCreatedDate()).isEqualTo(CREADO_EL);
            });
        }

        @Test
        @DisplayName("el recalculo edita la fila que ya existe y no inserta una segunda")
        void el_recalculo_no_inserta_una_segunda_fila() {
            // Cuida la copia de la version en el mapper. Si toJpa la dejara en null sobre
            // una entidad con id, Hibernate la tomaria por transitoria y escribiria una
            // fila nueva: el sintoma seria una violacion de uq_cam_month en una operacion
            // que no insertaba nada.
            CompanyActivityMonth guardado = repository.save(
                    mes(SchemaSeed.COMPANY_ID, MARZO, CommercialState.TRIAL, 3, 1, 8, "0.00"));
            entityManager.clear();

            CompanyActivityMonth cargado = repository.findById(guardado.getId()).orElseThrow();
            repository.save(cargado.recalculate(CommercialState.PAID, 19, 6, 211,
                    new BigDecimal("189000.00")));
            entityManager.clear();

            assertThat(repository.findAllByPeriodKey(MARZO.value(), 0, 20).content())
                    .singleElement()
                    .satisfies(unica -> assertThat(unica.getId()).isEqualTo(guardado.getId()));
        }
    }

    @Nested
    @DisplayName("Dormidos")
    class Dormidos {

        @Test
        @DisplayName("el barrido devuelve las que no pasan del umbral, y las mas dormidas primero")
        void el_barrido_devuelve_las_que_no_pasan_del_umbral() {
            // Las filas se siembran a mano porque HOY NO HAY NINGUN PROCESO QUE ALIMENTE
            // ESTA TABLA: sin siembra explicita el barrido devolveria vacio y el caso
            // pasaria en verde sin ejercitar ni el filtro ni el orden.
            repository.save(
                    mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 0, 0, 0, "189000.00"));
            repository.save(mes(SchemaSeed.OTRA_COMPANY_ID, ENERO, CommercialState.PAID, 2, 1, 3,
                    "89000.00"));
            insertarCruda(FILA_CRUDA + 20, SchemaSeed.COMPANY_ID, FEBRERO.value(), "PAID", 20, 9,
                    400, "189000.00");
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findDormant(ENERO.value(), 2, 0, 20).content())
                    .extracting(CompanyActivityMonth::getActiveDays)
                    // Cero arriba del todo: es el orden en que alguien va a atacar la
                    // lista. Y febrero no aparece: el barrido va de un mes concreto.
                    .containsExactly(0, 2);
        }

        @Test
        @DisplayName("el umbral excluye a la que lo supera, y el mes acota el barrido")
        void el_umbral_excluye_a_la_que_lo_supera() {
            repository.save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 20, 9, 400,
                    "189000.00"));
            repository.save(mes(SchemaSeed.OTRA_COMPANY_ID, ENERO, CommercialState.CHURNED, 1, 1, 0,
                    "0.00"));
            entityManager.clear();

            assertThat(repository.findDormant(ENERO.value(), 2, 0, 20).content())
                    .extracting(CompanyActivityMonth::getCompanyId)
                    .containsExactly(SchemaSeed.OTRA_COMPANY_ID);
            assertThat(repository.findDormant(MARZO.value(), 31, 0, 20).content()).isEmpty();
        }

        @Test
        @DisplayName("la busqueda por clinica y mes distingue el par que existe del que no")
        void la_busqueda_por_clinica_y_mes_distingue() {
            // Es la consulta detras de GET /lookup, y la que uq_cam_month garantiza que
            // devuelve como mucho una fila.
            repository.save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 21, 7, 143,
                    "189000.00"));
            entityManager.clear();

            assertThat(repository.findByCompanyIdAndPeriodKey(SchemaSeed.COMPANY_ID, ENERO.value()))
                    .isPresent();
            assertThat(repository.findByCompanyIdAndPeriodKey(SchemaSeed.OTRA_COMPANY_ID,
                    ENERO.value())).isEmpty();
            assertThat(repository.findByCompanyIdAndPeriodKey(SchemaSeed.COMPANY_ID, MARZO.value()))
                    .isEmpty();
        }

        @Test
        @DisplayName("la pagina acotada respeta el tope del kernel de paginacion")
        void la_pagina_acotada_respeta_el_tope() {
            repository.save(mes(SchemaSeed.COMPANY_ID, ENERO, CommercialState.PAID, 21, 7, 143,
                    "189000.00"));
            entityManager.clear();

            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            assertThat(repository.findAll(0, 100000).pageSize()).isEqualTo(200);
        }
    }

    private static CompanyActivityMonth mes(Long companyId, ActivityPeriodKey periodKey,
            CommercialState estado, int activeDays, int activeUsers, int recordsCreated,
            String mrr) {
        return CompanyActivityMonth.record(companyId, periodKey, estado, activeDays, activeUsers,
                recordsCreated, new BigDecimal(mrr), CREADO_EL);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para las comprobaciones que el
     * dominio ya replica: sin ella no habria forma de comprobar que la base tambien
     * las cuida —ni de ver el caso en que <em>no</em> las cuida—.
     */
    private void insertarCruda(Long id, Long companyId, String periodKey, String estado,
            int activeDays, int activeUsers, int recordsCreated, String mrr) {
        entityManager.createNativeQuery("""
                INSERT INTO company_activity_months (id, company_id, period_key, commercial_state,
                                                     active_days, active_users, records_created,
                                                     mrr_snapshot, created_date, version)
                VALUES (:id, :empresa, :periodo, :estado, :dias, :usuarios, :registros, :mrr,
                        :creado, 0)
                """).setParameter("id", id).setParameter("empresa", companyId)
                .setParameter("periodo", periodKey).setParameter("estado", estado)
                .setParameter("dias", activeDays).setParameter("usuarios", activeUsers)
                .setParameter("registros", recordsCreated).setParameter("mrr", new BigDecimal(mrr))
                .setParameter("creado", CREADO_EL).executeUpdate();
    }

}
