package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.domain.UsageBranch;
import com.vetsoftware.app.companyusageevent.domain.UsagePeriodKey;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCompanyUsageEventRepository} contra MySQL real.
 *
 * <p>
 * <b>Esta tabla es la unica que existe para GANAR una reclamacion de
 * excedente</b>, asi que lo que hay que vigilar aqui no es el ida y vuelta sino
 * las barandillas que solo sabe imponer el motor y que ninguna prueba de
 * servicio puede ejercitar:
 *
 * <ul>
 * <li><b>{@code uq_cue_fact}, a traves de la columna generada
 * {@code usage_ref_key}.</b> Sin ella el reintento del proceso de medicion
 * duplica el hecho, y con el el excedente facturado. Su correccion depende de
 * una condicion que no esta en el esquema sino en el codigo:
 * {@code occurred_at} tiene que ser el instante del <em>registro
 * consumido</em>. Los dos primeros casos de {@link Unicidad} son las dos
 * mitades de esa afirmacion —el mismo instante choca, otro instante cabe—, y
 * son lo que se pondria rojo el dia que alguien rellenara la columna con
 * {@code now()}.
 * <li><b>{@code chk_cue_branch}.</b> Hace dos cosas a la vez: impide que un
 * hecho apunte a la tabla equivocada, e impide que <em>exista</em> un hecho de
 * uso para un eje de existencias. Que lo diga el motor y no un comentario es la
 * diferencia entre una regla y un recuerdo.
 * <li><b>{@code chk_cue_billable} y {@code chk_cue_period_key}.</b> El primero
 * ata el cargo a la facturabilidad; el segundo admite tres granularidades y el
 * centinela, y solo se puede probar en crudo porque {@code UsagePeriodKey}
 * rechaza antes las formas invalidas.
 * </ul>
 *
 * <p>
 * <b>Se prueba sobre el eje {@code OWNER} a proposito.</b> Es la unica de las
 * cuatro ramas cuya cadena de claves foraneas cabe en un solo {@code INSERT}
 * —{@code owners} cuelga de ciudad y empresa, las dos ya sembradas—; las otras
 * tres arrastran especie, raza, agenda o documento electronico. La rama que se
 * ejercita no cambia lo que se afirma: {@code chk_cue_branch} es una sola
 * restriccion con cuatro ramas simetricas.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores para que todas las
 * rodajas compartan una unica clave de {@code MergedContextConfiguration} y,
 * con ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio
 * le daria a esta clase un arranque de contexto entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyUsageEventRepository — el hecho que sostiene el cobro, contra MySQL real")
class CompanyUsageEventPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja. */
    private static final Long DUENO_ID = 8600L;
    private static final Long OTRO_DUENO_ID = 8601L;
    private static final Long DUENO_AJENO_ID = 8602L;
    private static final Long CARGO_ID = 8610L;

    private static final LocalDateTime OCURRIO_EL = LocalDateTime.of(2026, 3, 14, 9, 30, 15);
    private static final LocalDateTime ANOTADO_EL = LocalDateTime.of(2026, 3, 14, 9, 30, 20);

    private static final UsagePeriodKey MARZO = UsagePeriodKey.of("2026-03");

    @Autowired
    private CompanyUsageEventJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaCompanyUsageEventRepository repository;
    private Long ejeDuenos;
    private Long ejeAnimales;
    private Long ejeUsuarios;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        ejeDuenos = SchemaSeed.limitDimensionId(entityManager, "OWNER");
        ejeAnimales = SchemaSeed.limitDimensionId(entityManager, "ANIMAL");
        ejeUsuarios = SchemaSeed.limitDimensionId(entityManager, "USER");
        dueno(DUENO_ID, "Dueno de prueba", SchemaSeed.COMPANY_ID);
        dueno(OTRO_DUENO_ID, "Segundo dueno", SchemaSeed.COMPANY_ID);
        dueno(DUENO_AJENO_ID, "Dueno ajeno", SchemaSeed.OTRA_COMPANY_ID);
        cargo();
        repository = new JpaCompanyUsageEventRepository(springDataRepository,
                new CompanyUsageEventJpaMapper());
    }

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("guarda el hecho y lo recupera con la rama, la referencia y el instante"
                + " en su sitio")
        void guarda_el_hecho_y_lo_recupera_campo_a_campo() {
            CompanyUsageEvent guardado = repository.save(hecho(DUENO_ID, OCURRIO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(recuperado.getLimitDimensionId()).isEqualTo(ejeDuenos);
                assertThat(recuperado.getBranch()).isEqualTo(UsageBranch.OWNER);
                assertThat(recuperado.getUsageReferenceId()).isEqualTo(DUENO_ID);
                assertThat(recuperado.getOccurredAt()).isEqualTo(OCURRIO_EL);
                assertThat(recuperado.getPeriodKey()).isEqualTo(MARZO);
                assertThat(recuperado.isBillable()).isTrue();
                assertThat(recuperado.getChargeId()).isNull();
                assertThat(recuperado.isCharged()).isFalse();
                assertThat(recuperado.getCreatedDate()).isEqualTo(ANOTADO_EL);
                assertThat(recuperado.getVersion()).isZero();
            });
        }

        /**
         * El mapper reparte la referencia a la columna de su rama y deja las otras tres
         * vacias. Se mira en crudo porque desde el dominio las cuatro columnas son un
         * solo campo, y un mapper que las cruzara devolveria exactamente lo mismo que
         * recibio.
         */
        @Test
        @DisplayName("la referencia aterriza en la columna de su rama y las otras tres quedan"
                + " vacias")
        void la_referencia_aterriza_en_la_columna_de_su_rama() {
            Long id = repository.save(hecho(DUENO_ID, OCURRIO_EL)).getId();
            entityManager.flush();

            Object[] fila = (Object[]) entityManager.createNativeQuery("""
                    SELECT usage_owner_id, usage_animal_id, usage_appointment_id,
                           usage_electronic_document_id, limit_dimension_code
                    FROM company_usage_events WHERE id = :id
                    """).setParameter("id", id).getSingleResult();

            assertThat(((Number) fila[0]).longValue()).isEqualTo(DUENO_ID);
            assertThat(fila[1]).isNull();
            assertThat(fila[2]).isNull();
            assertThat(fila[3]).isNull();
            assertThat(fila[4]).isEqualTo("OWNER");
        }

        /**
         * {@code usage_ref_key} es {@code GENERATED ALWAYS ... STORED} y no esta
         * mapeada. Este caso comprueba las dos mitades a la vez: que MySQL la calcula
         * aunque Java no la escriba —si estuviera mapeada, el {@code INSERT} la
         * nombraria y el motor lo rechazaria con el error 3105— y que su valor es el
         * que {@code uq_cue_fact} necesita para restringir.
         */
        @Test
        @DisplayName("usage_ref_key la calcula el motor: no esta mapeada y vale rama|referencia")
        void usage_ref_key_la_calcula_el_motor() {
            Long id = repository.save(hecho(DUENO_ID, OCURRIO_EL)).getId();
            entityManager.flush();

            Object clave = entityManager
                    .createNativeQuery(
                            "SELECT usage_ref_key FROM company_usage_events WHERE id = :id")
                    .setParameter("id", id).getSingleResult();

            assertThat(clave).isEqualTo("OWNER|" + DUENO_ID);
        }

        @Test
        @DisplayName("el listado por empresa no ve los hechos de otra clinica")
        void el_listado_por_empresa_no_ve_los_hechos_de_otra() {
            repository.save(hecho(DUENO_ID, OCURRIO_EL));
            repository.save(new CompanyUsageEvent(null, SchemaSeed.OTRA_COMPANY_ID, ejeDuenos,
                    UsageBranch.OWNER, DUENO_AJENO_ID, OCURRIO_EL, MARZO, true, null, ANOTADO_EL,
                    null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .singleElement().satisfies(
                            hecho -> assertThat(hecho.getUsageReferenceId()).isEqualTo(DUENO_ID));
        }

        @Test
        @DisplayName("el desglose por cargo devuelve solo los hechos que ese cargo facturo")
        void el_desglose_por_cargo_devuelve_solo_los_suyos() {
            CompanyUsageEvent cobrado = repository.save(hecho(DUENO_ID, OCURRIO_EL));
            repository.save(cobrado.attachToCharge(CARGO_ID));
            repository.save(hecho(OTRO_DUENO_ID, OCURRIO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(
                    repository.findAllByCompanyIdAndChargeId(SchemaSeed.COMPANY_ID, CARGO_ID, 0, 20)
                            .content())
                    .singleElement().satisfies(
                            hecho -> assertThat(hecho.getUsageReferenceId()).isEqualTo(DUENO_ID));
        }

        /**
         * {@code chk_cue_billable}. En crudo porque el dominio lo rechaza antes: un
         * hecho con cargo y no facturable es inconstruible desde Java, y lo que este
         * caso congela es que tampoco cabe por SQL directo.
         */
        @Test
        @DisplayName("un hecho con cargo pero no facturable lo rechaza el motor")
        void un_hecho_con_cargo_y_no_facturable_lo_rechaza_el_motor() {
            EngineConstraint.assertViolates("chk_cue_billable",
                    () -> insertarCrudo("OWNER", ejeDuenos, DUENO_ID, "2026-03", false, CARGO_ID));
        }
    }

    @Nested
    @DisplayName("Ramas")
    class Ramas {

        /**
         * El eje dice {@code ANIMAL} y la referencia esta en la columna de duenos. Es
         * la forma exacta del defecto que {@code chk_cue_branch} existe para impedir:
         * un hecho que cobra por la mascota de alguien apuntando a una fila de otra
         * tabla.
         *
         * <p>
         * <b>El identificador del eje es el de {@code ANIMAL} y no el de {@code OWNER},
         * y esa precision es lo que hace valido al caso.</b> {@code fk_cue_dimension}
         * es compuesta contra {@code limit_dimensions(id,
         * code)}: con el id del eje equivocado saltaria la clave foranea <em>antes</em>
         * que el {@code CHECK}, la prueba seguiria verde y estaria afirmando algo
         * distinto de lo que dice su nombre.
         */
        @Test
        @DisplayName("un hecho del eje ANIMAL con la referencia en la columna de duenos lo"
                + " rechaza el motor")
        void un_hecho_del_eje_ANIMAL_con_referencia_de_dueno_lo_rechaza_el_motor() {
            EngineConstraint.assertViolates("chk_cue_branch",
                    () -> insertarCrudo("ANIMAL", ejeAnimales, DUENO_ID, "2026-03", true, null));
        }

        /**
         * <b>No existe hecho de uso para un eje de existencias.</b> {@code USER},
         * {@code BRANCH}, {@code TERMINAL} y {@code STORAGE_GB} se <em>cuentan</em> —el
         * contador vive en {@code company_capacities}—, no se acumulan hecho a hecho.
         * Sin esta restriccion, el mismo consumo se podria sumar dos veces por dos
         * caminos distintos.
         */
        @Test
        @DisplayName("un eje de existencias como USER no puede tener hecho de uso")
        void un_eje_de_existencias_no_puede_tener_hecho_de_uso() {
            EngineConstraint.assertViolates("chk_cue_branch",
                    () -> insertarCrudo("USER", ejeUsuarios, DUENO_ID, "ALLTIME", true, null));
        }

        @Test
        @DisplayName("un hecho sin ninguna referencia informada lo rechaza el motor")
        void un_hecho_sin_referencia_lo_rechaza_el_motor() {
            EngineConstraint.assertViolates("chk_cue_branch",
                    () -> entityManager.createNativeQuery("""
                            INSERT INTO company_usage_events (company_id, limit_dimension_id,
                                    limit_dimension_code, occurred_at, period_key, billable,
                                    created_date, version)
                            VALUES (:empresa, :eje, 'OWNER', :ocurrio, '2026-03', TRUE, NOW(6), 0)
                            """).setParameter("empresa", SchemaSeed.COMPANY_ID)
                            .setParameter("eje", ejeDuenos).setParameter("ocurrio", OCURRIO_EL)
                            .executeUpdate());
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        /**
         * <b>La mitad que sostiene el excedente facturado.</b> El proceso de medicion
         * puede morir a mitad de lote y volver a pasar por los mismos registros; sin
         * {@code uq_cue_fact} cada reintento duplicaria el hecho y con el la
         * reclamacion.
         */
        @Test
        @DisplayName("el reintento del proceso de medicion, con el mismo occurred_at, choca")
        void el_reintento_con_el_mismo_instante_choca() {
            repository.save(hecho(DUENO_ID, OCURRIO_EL));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_cue_fact", () -> {
                repository.save(hecho(DUENO_ID, OCURRIO_EL));
                entityManager.flush();
            });
        }

        /**
         * <b>La otra mitad, y la condicion de la que depende toda la proteccion.</b>
         * {@code occurred_at} es el instante del REGISTRO CONSUMIDO, no el del reloj
         * del proceso: por eso un ajuste por reconteo —que es un hecho distinto, con su
         * propio instante— si cabe. El dia que alguien rellene esa columna con
         * {@code now()}, el reintento dejara de chocar y la proteccion desaparecera
         * <b>sin un solo error</b>; este caso seguiria verde y el anterior se pondria
         * rojo, que es justo la senal que hay que mirar.
         */
        @Test
        @DisplayName("un reconteo con otro occurred_at si cabe: si alguien pusiera now() ahi,"
                + " el reintento dejaria de chocar y nada avisaria")
        void un_reconteo_con_otro_instante_si_cabe() {
            repository.save(hecho(DUENO_ID, OCURRIO_EL));
            repository.save(hecho(DUENO_ID, OCURRIO_EL.plusMinutes(1)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).totalElements())
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("el mismo instante sobre otro registro consumido no choca")
        void el_mismo_instante_sobre_otra_referencia_no_choca() {
            repository.save(hecho(DUENO_ID, OCURRIO_EL));
            repository.save(hecho(OTRO_DUENO_ID, OCURRIO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).totalElements())
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Periodo")
    class Periodo {

        /**
         * Las tres granularidades y el centinela. Van sobre el mismo dueno con
         * instantes distintos para no chocar con {@code uq_cue_fact}, que es
         * precisamente lo que prueba el bloque anterior.
         */
        @Test
        @DisplayName("acepta las tres granularidades reales y el centinela ALLTIME")
        void acepta_las_cuatro_formas() {
            repository.save(hecho(DUENO_ID, OCURRIO_EL, UsagePeriodKey.of("2026-03")));
            repository
                    .save(hecho(DUENO_ID, OCURRIO_EL.plusMinutes(1), UsagePeriodKey.of("2026-Q3")));
            repository
                    .save(hecho(DUENO_ID, OCURRIO_EL.plusMinutes(2), UsagePeriodKey.of("2026-S1")));
            repository
                    .save(hecho(DUENO_ID, OCURRIO_EL.plusMinutes(3), UsagePeriodKey.of("ALLTIME")));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(hecho -> hecho.getPeriodKey().value())
                    .containsExactlyInAnyOrder("2026-03", "2026-Q3", "2026-S1", "ALLTIME");
        }

        /**
         * En crudo porque {@code UsagePeriodKey} lo rechaza antes de llegar al motor.
         * Lo que congela este caso es que la barandilla existe <b>tambien</b> en la
         * base: sin ella, una carga por SQL directo meteria un mes trece y el cupo se
         * repartiria entre dos periodos que nadie sabe leer.
         */
        @Test
        @DisplayName("un mes trece lo rechaza el motor, no solo el dominio")
        void un_mes_trece_lo_rechaza_el_motor() {
            EngineConstraint.assertViolates("chk_cue_period_key",
                    () -> insertarCrudo("OWNER", ejeDuenos, DUENO_ID, "2026-13", true, null));
        }
    }

    // ── andamio ──────────────────────────────────────────────────────────────

    private CompanyUsageEvent hecho(Long referencia, LocalDateTime ocurrio) {
        return hecho(referencia, ocurrio, MARZO);
    }

    private CompanyUsageEvent hecho(Long referencia, LocalDateTime ocurrio,
            UsagePeriodKey periodo) {
        return CompanyUsageEvent.record(SchemaSeed.COMPANY_ID, ejeDuenos, UsageBranch.OWNER,
                referencia, ocurrio, periodo, true, ANOTADO_EL);
    }

    /**
     * El {@code INSERT} en crudo que el dominio no deja construir. Siempre escribe
     * la referencia en {@code usage_owner_id}: lo que varia entre casos es el
     * codigo del eje, el periodo, la facturabilidad y el cargo.
     */
    private void insertarCrudo(String codigoEje, Long ejeId, Long dueno, String periodo,
            boolean facturable, Long cargo) {
        entityManager.createNativeQuery("""
                INSERT INTO company_usage_events (company_id, limit_dimension_id,
                        limit_dimension_code, usage_owner_id, occurred_at, period_key, billable,
                        charge_id, created_date, version)
                VALUES (:empresa, :eje, :codigo, :dueno, :ocurrio, :periodo, :facturable, :cargo,
                        NOW(6), 0)
                """).setParameter("empresa", SchemaSeed.COMPANY_ID).setParameter("eje", ejeId)
                .setParameter("codigo", codigoEje).setParameter("dueno", dueno)
                .setParameter("ocurrio", OCURRIO_EL).setParameter("periodo", periodo)
                .setParameter("facturable", facturable).setParameter("cargo", cargo)
                .executeUpdate();
    }

    /**
     * Solo las columnas obligatorias sin defecto. La ciudad es la del andamio, que
     * ya lleva un codigo DANE sintetico: {@code uq_cities_dane_code} es GLOBAL y
     * los codigos reales chocan entre rodajas. {@code document_type} y
     * {@code person_type} son NOT NULL sin valor por defecto desde el changeset 117
     * ({@code addNotNullConstraint} sin default): sin ellas el INSERT muere con
     * "Field 'document_type' doesn't have a default value". Mismos valores que usa
     * {@code ClinicalEventPersistenceIT.sembrarLaCadenaCompleta}.
     */
    private void dueno(Long id, String nombre, Long empresa) {
        entityManager.createNativeQuery("""
                INSERT INTO owners (id, name, document, document_type, person_type, city_id,
                                    company_id, created_date)
                VALUES (:id, :nombre, :documento, 'CEDULA_CIUDADANIA', 'NATURAL', :ciudad,
                        :empresa, NOW())
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("documento", "DOC-" + id).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", empresa).executeUpdate();
    }

    /**
     * Un cargo {@code ONE_TIME} y no {@code RECURRING} a proposito: el recurrente
     * arrastra {@code chk_subscription_charges_recurring_item} y la columna
     * generada {@code recurring_charge_key} del changeset 372, que no tienen nada
     * que ver con lo que aqui se prueba.
     */
    private void cargo() {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_charges (id, company_id, subscription_id, charge_type,
                        description, service_period_start, service_period_end, quantity,
                        unit_amount, subtotal_amount, tax_rate, tax_treatment, status,
                        created_date)
                VALUES (:id, :empresa, :contrato, 'ONE_TIME', 'Cargo de andamio', '2026-03-01',
                        '2026-03-31', 1.000, 1000.00, 1000.00, 0.00, 'EXCLUDED', 'PENDING', NOW())
                """).setParameter("id", CARGO_ID).setParameter("empresa", SchemaSeed.COMPANY_ID)
                .setParameter("contrato", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
    }
}
