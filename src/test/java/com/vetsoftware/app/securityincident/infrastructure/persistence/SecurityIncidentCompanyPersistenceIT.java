package com.vetsoftware.app.securityincident.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentCompanyRepository;
import com.vetsoftware.app.securityincident.domain.AffectedScope;
import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaSecurityIncidentCompanyRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es que el ambito entre en la
 * unicidad.</b> El documento maestro proponia
 * {@code (security_incident_id, company_id)}; el changeset 357 le anadio
 * {@code affected_scope} porque un ataque que expone credenciales <em>y</em>
 * datos clinicos de la misma clinica son dos hechos con dos alcances, y con la
 * clave corta el segundo era <b>inescribible</b>. Los dos casos de
 * {@link Unicidad} son las dos mitades de esa decision: la misma clinica dos
 * veces con ambitos distintos entra, y el mismo ambito repetido choca. Sin el
 * primero, nadie notaria el dia que alguien "simplifique" la clave.
 *
 * <p>
 * <b>Y lo segundo es una ausencia.</b> El puerto no declara borrado —ni la
 * entidad lleva {@code @SQLDelete}, ni el controller publica un {@code DELETE}—
 * porque quitar una clinica de la lista de afectados destruye la prueba de que
 * se le notifico. {@link SinBorrado} congela esa ausencia: es una invariante
 * estructural, y sin un caso que la afirme volveria a entrar en el primer PR
 * que "complete el CRUD".
 *
 * <p>
 * <b>La empresa va como escalar y no como {@code @ManyToOne}</b>, asi que
 * {@code fk_sic_company} vigila en la base y no hay navegacion desde Java. Por
 * eso las dos empresas de los casos son las que siembra {@code SchemaSeed}: sin
 * fila real en {@code companies} la clave foranea rechaza el insert.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano:</b> mismo motivo que en
 * {@link SecurityIncidentPersistenceIT}. Un {@code @Import} propio le daria a
 * esta clase una clave de contexto unica y un arranque entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSecurityIncidentCompanyRepository — la puente de afectados contra MySQL real")
class SecurityIncidentCompanyPersistenceIT extends AbstractDataJpaTest {

    /** Id del rango reservado a esta rodaja, para las escrituras crudas. */
    private static final Long AFECTADO_CRUDO = 8650L;

    private static final LocalDateTime DETECTADO = LocalDateTime.of(2026, 3, 3, 8, 30, 0);
    private static final LocalDateTime ESCALADO = LocalDateTime.of(2026, 3, 5, 9, 0, 0);
    private static final LocalDateTime VENCE = LocalDateTime.of(2026, 3, 26, 23, 59, 59,
            999_999_000);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 5, 9, 5, 0);

    @Autowired
    private SecurityIncidentCompanyJpaRepository springDataRepository;
    @Autowired
    private SecurityIncidentJpaRepository incidentJpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaSecurityIncidentCompanyRepository repository;
    private Long incidenteId;

    @BeforeEach
    void adaptador() {
        SchemaSeed.seed(entityManager);
        repository = new JpaSecurityIncidentCompanyRepository(springDataRepository,
                incidentJpaRepository, new SecurityIncidentCompanyJpaMapper());
        incidenteId = new JpaSecurityIncidentRepository(incidentJpaRepository,
                new SecurityIncidentJpaMapper()).save(incidente()).getId();
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la clinica alcanzada y la recupera con su ambito y su contador")
        void guarda_la_clinica_alcanzada_y_la_recupera() {
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.PERSONAL_DATA, 320));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIncidentId(incidenteId, 0, 20).content()).singleElement()
                    .satisfies(fila -> {
                        assertThat(fila.getSecurityIncidentId()).isEqualTo(incidenteId);
                        assertThat(fila.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(fila.getAffectedScope()).isEqualTo(AffectedScope.PERSONAL_DATA);
                        // El contador es el DE ESA CLINICA, no el del incidente entero:
                        // el total declarado vive en la tabla madre.
                        assertThat(fila.getAffectedSubjectCount()).isEqualTo(320);
                    });
        }

        @Test
        @DisplayName("el listado ordena por clinica y despues por ambito, con el id de desempate")
        void el_listado_ordena_por_clinica_y_despues_por_ambito() {
            // La misma clinica aparece dos veces, asi que ordenar solo por empresa NO
            // seria un orden total y dos paginas consecutivas podrian repetir u omitir.
            repository.save(afectada(SchemaSeed.OTRA_COMPANY_ID, AffectedScope.BILLING_DATA, 10));
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.PERSONAL_DATA, 20));
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.CREDENTIALS, 30));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIncidentId(incidenteId, 0, 20).content())
                    .extracting(SecurityIncidentCompany::getCompanyId,
                            SecurityIncidentCompany::getAffectedScope)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(SchemaSeed.COMPANY_ID,
                                    AffectedScope.CREDENTIALS),
                            org.assertj.core.groups.Tuple.tuple(SchemaSeed.COMPANY_ID,
                                    AffectedScope.PERSONAL_DATA),
                            org.assertj.core.groups.Tuple.tuple(SchemaSeed.OTRA_COMPANY_ID,
                                    AffectedScope.BILLING_DATA));
        }

        @Test
        @DisplayName("el listado de otro incidente no arrastra las filas de este")
        void el_listado_de_otro_incidente_no_arrastra_estas_filas() {
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.PERSONAL_DATA, 320));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIncidentId(incidenteId + 100_000L, 0, 20).content())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("la misma clinica SI cabe dos veces con dos ambitos distintos")
        void la_misma_clinica_cabe_dos_veces_con_dos_ambitos_distintos() {
            // ES LA DECISION DEL CHANGESET 357, y el caso que se pondria rojo si
            // alguien "simplificara" uq_sic_pair a (incidente, empresa): un ataque que
            // expone credenciales Y datos clinicos de la misma clinica son dos hechos
            // con dos alcances, y con la clave corta el segundo es inescribible.
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.CREDENTIALS, 15));
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.CLINICAL_DATA, 480));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIncidentId(incidenteId, 0, 20).content())
                    .extracting(SecurityIncidentCompany::getAffectedScope)
                    .containsExactlyInAnyOrder(AffectedScope.CREDENTIALS,
                            AffectedScope.CLINICAL_DATA);
        }

        @Test
        @DisplayName("la misma clinica con el MISMO ambito choca contra uq_sic_pair")
        void la_misma_clinica_con_el_mismo_ambito_choca() {
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.CREDENTIALS, 15));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_sic_pair", () -> {
                repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.CREDENTIALS, 99));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la comprobacion previa distingue la terna que existe de la que no")
        void la_comprobacion_previa_distingue_la_terna_que_existe() {
            // Es el metodo que el caso de uso consulta ANTES de insertar, para dar el
            // conflicto con su nombre en vez de un Duplicate entry del driver.
            repository.save(afectada(SchemaSeed.COMPANY_ID, AffectedScope.CREDENTIALS, 15));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsByIncidentIdAndCompanyIdAndScope(incidenteId,
                    SchemaSeed.COMPANY_ID, AffectedScope.CREDENTIALS)).isTrue();
            // Mismo incidente y misma clinica, otro ambito: NO existe. Si la unicidad
            // perdiera el ambito, esta linea empezaria a mentir.
            assertThat(repository.existsByIncidentIdAndCompanyIdAndScope(incidenteId,
                    SchemaSeed.COMPANY_ID, AffectedScope.CLINICAL_DATA)).isFalse();
            assertThat(repository.existsByIncidentIdAndCompanyIdAndScope(incidenteId,
                    SchemaSeed.OTRA_COMPANY_ID, AffectedScope.CREDENTIALS)).isFalse();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un ambito fuera de los cuatro lo para chk_sic_scope")
        void un_ambito_desconocido_lo_para_el_motor() {
            // La red del enum: si alguien anade un valor a AffectedScope sin el
            // changeset que lo admita, el INSERT muere en vez de entrar como texto.
            EngineConstraint.assertViolates("chk_sic_scope",
                    () -> insertarCruda(AFECTADO_CRUDO, "SUPPLIER_DATA", 10));
        }

        @Test
        @DisplayName("un contador de titulares negativo lo para chk_sic_count")
        void un_contador_negativo_lo_para_el_motor() {
            EngineConstraint.assertViolates("chk_sic_count",
                    () -> insertarCruda(AFECTADO_CRUDO + 1, "PERSONAL_DATA", -1));
        }

        @Test
        @DisplayName("una clinica que no existe la para fk_sic_company")
        void una_clinica_inexistente_la_para_la_clave_foranea() {
            // La empresa va como escalar y no como @ManyToOne —un @ManyToOne a
            // CompanyJpaEntity activaria las cuatro reglas BE-COV sobre la feature
            // entera—, asi que la clave foranea es lo unico que impide colgar el
            // incidente de una clinica inventada. Este caso comprueba que sigue viva.
            EngineConstraint.assertViolates("fk_sic_company",
                    () -> insertarCrudaConEmpresa(AFECTADO_CRUDO + 2, 999999L));
        }
    }

    @Nested
    @DisplayName("Sin borrado")
    class SinBorrado {

        @Test
        @DisplayName("el puerto no declara ninguna operacion de borrado, y esa ausencia es la "
                + "decision")
        void el_puerto_no_declara_borrado() {
            // Quitar una clinica de la lista destruye la prueba de que se le notifico.
            // La ausencia es estructural y no una casualidad: sin este caso volveria a
            // entrar en el primer PR que "complete el CRUD", y nada lo detendria.
            assertThat(Arrays.stream(SecurityIncidentCompanyRepository.class.getMethods())
                    .map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove")
                            || nombre.startsWith("disable"));
        }
    }

    private static SecurityIncident incidente() {
        return SecurityIncident.register(DETECTADO, null, ESCALADO,
                SecurityIncidentKind.UNAUTHORIZED_ACCESS, IncidentSeverity.CRITICAL,
                "Acceso no autorizado a la consola de administracion", 800, VENCE, CREADO_EL);
    }

    private SecurityIncidentCompany afectada(Long companyId, AffectedScope ambito, int titulares) {
        return SecurityIncidentCompany.register(incidenteId, companyId, ambito, titulares);
    }

    private void insertarCruda(Long id, String ambito, int titulares) {
        insertar(id, SchemaSeed.COMPANY_ID, ambito, titulares);
    }

    private void insertarCrudaConEmpresa(Long id, Long companyId) {
        insertar(id, companyId, "PERSONAL_DATA", 10);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para las comprobaciones que el
     * dominio ya replica: sin ella no habria forma de comprobar que la base tambien
     * las cuida.
     */
    private void insertar(Long id, Long companyId, String ambito, int titulares) {
        entityManager.createNativeQuery("""
                INSERT INTO security_incident_companies (id, security_incident_id, company_id,
                                                         affected_scope, affected_subject_count)
                VALUES (:id, :incidente, :empresa, :ambito, :titulares)
                """).setParameter("id", id).setParameter("incidente", incidenteId)
                .setParameter("empresa", companyId).setParameter("ambito", ambito)
                .setParameter("titulares", titulares).executeUpdate();
    }
}
