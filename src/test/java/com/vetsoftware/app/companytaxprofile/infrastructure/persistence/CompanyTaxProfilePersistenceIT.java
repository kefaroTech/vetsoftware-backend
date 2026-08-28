package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del perfil fiscal contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Nada de lo que sostiene esta feature
 * es codigo Java:
 *
 * <ul>
 * <li>Desde el changeset 364 la unicidad ya <em>no</em> es {@code company_id} a
 * secas -la tabla guarda el historico de fichas- sino
 * {@code uq_company_tax_profiles_current} sobre la columna generada
 * {@code current_profile_marker}: una empresa puede tener muchas fichas y una
 * sola vigente. Y sigue sin mirar {@code enabled}. Eso lo decide el indice, no
 * el caso de uso.
 * <li>El {@code @SQLRestriction} de las dos tablas y el {@code @EntityGraph}
 * que hidrata empresa, actividad y responsabilidades en una sola consulta.
 * <li>Los dos enums viajan como texto a columnas {@code VARCHAR(30)}.
 * </ul>
 *
 * <p>
 * <b>Las cuentas se hacen con SQL nativo y sin filtros.</b> Contar las
 * responsabilidades por el adaptador o por su entidad JPA haria que un cero
 * pudiera significar «filtradas por {@code @SQLRestriction}» en vez de
 * «borradas de la base»: justo la distincion que este test existe para hacer. Y
 * aqui la cuenta que detecta el bug es la de responsabilidades
 * <em>vigentes</em>, no la de filas: la hija tiene su propio
 * {@code @SQLDelete}, asi que el cascade no arranca la fila sino que la apaga
 * (ver {@link #responsabilidadesVigentesEnLaBase(Long)}).
 *
 * <p>
 * {@link SchemaSeed} no siembra actividades economicas y
 * {@code company_tax_profiles} tiene FK a {@code economic_activities}, asi que
 * se insertan aqui por SQL nativo con ids propios (960-961) que no chocan con
 * los suyos. Las constantes del perfil se reusan de
 * {@link CompanyTaxProfileMother}, pero la {@link CompanyRef} es la de la
 * empresa sembrada: la del mother apunta a una empresa que no existe en la
 * base.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyTaxProfileRepository — perfil fiscal, responsabilidades y baja contra MySQL real")
class CompanyTaxProfilePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long ACTIVIDAD_ID = 960L;
    private static final Long OTRA_ACTIVIDAD_ID = 961L;

    private static final CompanyRef CLINICA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef CLINICA_AJENA = new CompanyRef(OTRA_COMPANY,
            "Veterinaria ajena", "900654321");
    private static final EconomicActivityRef VETERINARIA = new EconomicActivityRef(ACTIVIDAD_ID,
            "7500", "Actividades veterinarias");
    private static final EconomicActivityRef COMERCIO = new EconomicActivityRef(OTRA_ACTIVIDAD_ID,
            "4773", "Comercio al por menor de otros productos nuevos");

    private static final CompanyTaxProfileResponsibility O13 = CompanyTaxProfileMother.O13;
    private static final CompanyTaxProfileResponsibility O15 = CompanyTaxProfileMother.O15;
    private static final CompanyTaxProfileResponsibility O23 = new CompanyTaxProfileResponsibility(
            "O-23");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaCompanyTaxProfileRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        actividad(ACTIVIDAD_ID, "7500", "Actividades veterinarias");
        actividad(OTRA_ACTIVIDAD_ID, "4773", "Comercio al por menor de otros productos nuevos");
        entityManager.flush();

        // Guardia de la siembra. Todo va con INSERT IGNORE y MySQL degrada a warning
        // tanto el NOT NULL sin valor como la violacion de FK: la fila no se inserta y
        // nadie se entera hasta que revienta una FK dos tablas mas abajo, apuntando a
        // la tabla equivocada. Aqui falla donde de verdad falta el dato.
        assertThat(filas("companies", COMPANY)).as("la empresa dueña del perfil").isOne();
        assertThat(filas("companies", OTRA_COMPANY)).as("la empresa del otro tenant").isOne();
        assertThat(filas("economic_activities", ACTIVIDAD_ID)).as("la actividad economica CIIU")
                .isOne();
        assertThat(filas("economic_activities", OTRA_ACTIVIDAD_ID))
                .as("la actividad economica alterna").isOne();
    }

    /** {@code SchemaSeed} no llega al catalogo CIIU y la FK del perfil si. */
    private void actividad(Long id, String codigo, String nombre) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO economic_activities (id, code, name, created_date, enabled)
                VALUES (:id, :code, :name, '2026-01-15 08:00:00', true)
                """).setParameter("id", id).setParameter("code", codigo)
                .setParameter("name", nombre).executeUpdate();
    }

    private long filas(String tabla, Long id) {
        Number total = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .setParameter("id", id).getSingleResult();
        return total.longValue();
    }

    // --- helpers del agregado ------------------------------------------------

    private CompanyTaxProfile perfil(CompanyRef empresa, EconomicActivityRef actividad,
            TaxRegime regimen, List<CompanyTaxProfileResponsibility> responsabilidades) {
        return new CompanyTaxProfile(null, empresa, CompanyDocumentType.NIT,
                CompanyTaxProfileMother.NIT, CompanyTaxProfileMother.NIT_DV,
                CompanyTaxProfileMother.RAZON_SOCIAL, regimen, CompanyTaxProfileMother.EMAIL_FISCAL,
                CompanyTaxProfileMother.NOMBRE_COMERCIAL, actividad, responsabilidades,
                CREADO.toLocalDate(), null, CREADO, null, true);
    }

    /**
     * Perfil por defecto de la empresa propia: NIT, actividad y dos
     * responsabilidades.
     */
    private CompanyTaxProfile guardarPerfilPropio() {
        return repository
                .save(perfil(CLINICA, VETERINARIA, TaxRegime.RESPONSABLE_IVA, List.of(O13, O15)));
    }

    private CompanyTaxProfile guardarPerfilAjeno() {
        return repository
                .save(perfil(CLINICA_AJENA, COMERCIO, TaxRegime.NO_RESPONSABLE_IVA, List.of(O23)));
    }

    /** Vacia el contexto de persistencia: obliga a que la lectura vaya a MySQL. */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Filas de detalle contadas con SQL nativo y sin filtros de Hibernate: es la
     * unica forma de distinguir «escondida» de «borrada».
     */
    private long responsabilidadesEnLaBase(Long perfilId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM company_tax_profile_responsibilities
                 WHERE company_tax_profile_id = :id
                """).setParameter("id", perfilId).getSingleResult();
        return ((Number) total).longValue();
    }

    /**
     * Las que siguen vigentes, tambien por SQL nativo.
     *
     * <p>
     * <b>Esta es la cuenta que detecta el bug del cascade en esta feature</b>, y no
     * la de filas totales: a diferencia de las lineas de una orden o de los abonos
     * de una factura, {@code CompanyTaxProfileResponsibilityJpaEntity} tiene su
     * propio {@code @SQLDelete}, asi que el borrado en cascada del
     * {@code orphanRemoval} no arranca la fila: la deja con
     * {@code enabled = false}. Contar filas totales daria verde con y sin el
     * arreglo; contar las vigentes distingue las dos cosas, porque el UPDATE nativo
     * de la cabecera no toca esta columna y el cascade si.
     */
    /** El {@code valid_to} tal y como quedo en la fila, sin pasar por el mapper. */
    private String validToEnLaBase(Long perfilId) {
        entityManager.flush();
        Object valor = entityManager.createNativeQuery("""
                SELECT CAST(valid_to AS CHAR) FROM company_tax_profiles WHERE id = :id
                """).setParameter("id", perfilId).getSingleResult();
        return valor == null ? null : valor.toString();
    }

    /** La {@code version} tal y como quedo en la fila, sin pasar por el mapper. */
    private long versionEnLaBase(Long perfilId) {
        entityManager.flush();
        Object valor = entityManager
                .createNativeQuery("SELECT version FROM company_tax_profiles WHERE id = :id")
                .setParameter("id", perfilId).getSingleResult();
        return ((Number) valor).longValue();
    }

    /**
     * Ficha ya cerrada construida a mano con id y empresa elegidos: es lo unico que
     * {@code close} mira -{@code id}, {@code company.id()} y {@code validTo}-, y
     * construirla asi permite pedir el cierre con la empresa equivocada sin que el
     * dominio lo impida antes de llegar al {@code UPDATE}.
     */
    private CompanyTaxProfile perfilCerrado(Long id, CompanyRef empresa, LocalDate hasta) {
        return new CompanyTaxProfile(id, empresa, CompanyDocumentType.NIT,
                CompanyTaxProfileMother.NIT, CompanyTaxProfileMother.NIT_DV,
                CompanyTaxProfileMother.RAZON_SOCIAL, TaxRegime.RESPONSABLE_IVA,
                CompanyTaxProfileMother.EMAIL_FISCAL, null, null, List.of(), CREADO.toLocalDate(),
                hasta, CREADO, null, true);
    }

    private long responsabilidadesVigentesEnLaBase(Long perfilId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM company_tax_profile_responsibilities
                 WHERE company_tax_profile_id = :id AND enabled = true
                """).setParameter("id", perfilId).getSingleResult();
        return ((Number) total).longValue();
    }

    // --- casos ---------------------------------------------------------------

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer devuelve el perfil con sus refs y sus responsabilidades")
        void guardar_y_releer_devuelve_el_perfil_completo() {
            CompanyTaxProfile guardado = guardarPerfilPropio();
            releerDesdeLaBase();

            CompanyTaxProfile leido = repository.findCurrentByCompanyId(COMPANY).orElseThrow();

            assertThat(leido.getId()).isEqualTo(guardado.getId());
            assertThat(leido.getCompany()).isEqualTo(CLINICA);
            assertThat(leido.getCompanyDocumentType()).isEqualTo(CompanyDocumentType.NIT);
            assertThat(leido.getCompanyDocumentId()).isEqualTo(CompanyTaxProfileMother.NIT);
            assertThat(leido.getCompanyDocumentVerificationDigit())
                    .isEqualTo(CompanyTaxProfileMother.NIT_DV);
            assertThat(leido.getLegalName()).isEqualTo(CompanyTaxProfileMother.RAZON_SOCIAL);
            assertThat(leido.getTaxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(leido.getFiscalEmail()).isEqualTo(CompanyTaxProfileMother.EMAIL_FISCAL);
            assertThat(leido.getCommercialName())
                    .isEqualTo(CompanyTaxProfileMother.NOMBRE_COMERCIAL);
            assertThat(leido.getEconomicActivity()).isEqualTo(VETERINARIA);
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getResponsibilities())
                    .extracting(CompanyTaxProfileResponsibility::code)
                    .containsExactlyInAnyOrder("O-13", "O-15");
        }

        @Test
        @DisplayName("el save devuelve el id generado de la cabecera")
        void el_save_devuelve_el_id_generado() {
            assertThat(guardarPerfilPropio().getId()).isNotNull();
        }

        @Test
        @DisplayName("sin actividad economica ni responsabilidades la FK vuelve nula y la lista vacia")
        void sin_actividad_ni_responsabilidades_vuelve_pelado() {
            // economic_activity_id es la unica FK opcional del perfil: una persona
            // natural no la declara. Si la columna dejara de admitir nulos, el registro
            // fiscal de medio catalogo de clientes seria imposible de guardar.
            repository.save(perfil(CLINICA, null, TaxRegime.NO_RESPONSABLE_IVA, List.of()));
            releerDesdeLaBase();

            CompanyTaxProfile leido = repository.findCurrentByCompanyId(COMPANY).orElseThrow();

            assertThat(leido.getEconomicActivity()).isNull();
            assertThat(leido.getResponsibilities()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(TaxRegime.class)
        @DisplayName("cada regimen se relee tal cual desde la columna VARCHAR")
        void cada_regimen_se_relee_tal_cual(TaxRegime regimen) {
            // @Enumerated(STRING): con ORDINAL, un enum reordenado convertiria a un
            // responsable de IVA en no responsable sin tocar una fila.
            repository.save(perfil(CLINICA, VETERINARIA, regimen, List.of(O13)));
            releerDesdeLaBase();

            assertThat(repository.findCurrentByCompanyId(COMPANY))
                    .map(CompanyTaxProfile::getTaxRegime).contains(regimen);
        }

        @Test
        @DisplayName("suceder el perfil cierra el vigente y deja el nuevo como unico vigente")
        void suceder_cierra_el_vigente_y_abre_el_nuevo() {
            // Desde el changeset 364 el cambio de datos fiscales NO reescribe la fila:
            // cierra la vigente con valid_to y abre otra. Las dos verdades quedan, que es
            // lo unico que hace que una factura de hace un ano siga diciendo con que
            // identidad se emitio.
            Long anterior = guardarPerfilPropio().getId();
            releerDesdeLaBase();
            CompanyTaxProfile aCerrar = repository.findCurrentByCompanyId(COMPANY).orElseThrow();

            aCerrar.closeOn(CREADO.toLocalDate().plusDays(1));
            // close y no save: guardar el agregado entero reinsertaria sus
            // responsabilidades y chocaria con uq_ctp_responsibilities_profile_code.
            assertThat(repository.close(aCerrar)).isOne();
            CompanyTaxProfile sucesor = repository.save(CompanyTaxProfile.open(CLINICA,
                    CompanyDocumentType.NIT, CompanyTaxProfileMother.NIT,
                    CompanyTaxProfileMother.NIT_DV, "Clinica Veterinaria Sur S.A.S.",
                    TaxRegime.NO_RESPONSABLE_IVA, "contabilidad@vetsur.com", "Vet Sur", COMERCIO,
                    List.of(O23), CREADO.toLocalDate().plusDays(1), CREADO.plusDays(1)));
            releerDesdeLaBase();

            CompanyTaxProfile leido = repository.findCurrentByCompanyId(COMPANY).orElseThrow();
            assertThat(leido.getId()).isEqualTo(sucesor.getId()).isNotEqualTo(anterior);
            assertThat(leido.getValidTo()).isNull();
            assertThat(leido.getLegalName()).isEqualTo("Clinica Veterinaria Sur S.A.S.");
            assertThat(leido.getEconomicActivity()).isEqualTo(COMERCIO);
            assertThat(leido.getResponsibilities())
                    .extracting(CompanyTaxProfileResponsibility::code).containsExactly("O-23");
            // La ficha anterior sigue entera y con sus responsabilidades vivas: es lo que
            // el documento de entonces necesita para explicarse.
            assertThat(responsabilidadesVigentesEnLaBase(anterior)).isEqualTo(2L);
            assertThat(validToEnLaBase(anterior))
                    .isEqualTo(CREADO.toLocalDate().plusDays(1).toString());
        }
    }

    /**
     * <b>El camino de aborto de {@code close}, que es el que no tenia prueba.</b>
     *
     * <p>
     * {@code closeCurrent} escribe una sola columna y su {@code WHERE} tiene tres
     * filtros: {@code id}, {@code company_id} y {@code valid_to IS NULL}. Devolver
     * <b>cero filas</b> es la unica senal que tiene el caso de uso para saber que
     * no debe insertar la sucesora, y seguir adelante dejaria dos fichas vigentes:
     * quien lo parara seria {@code uq_company_tax_profiles_current} con un
     * {@code Duplicate entry} sobre una columna generada que no aparece en ningun
     * sitio del codigo Java.
     *
     * <p>
     * <b>Los dos motivos reales del cero son estos dos, y ninguno es la
     * version.</b> {@code version} va en el {@code SET} —{@code version = version +
     * 1}, regla {@code UPDATE_MASIVO_MUEVE_LA_VERSION}— y <em>no</em> en el
     * {@code WHERE}: una version obsoleta no produce cero filas aqui. Lo que hace
     * el incremento es que un {@code save} concurrente salido de una lectura
     * anterior no pueda pisar el cierre sin ruido, y eso se comprueba en
     * {@link #cerrar_mueve_la_version_de_la_fila()}.
     */
    @Nested
    @DisplayName("el cierre que no afecta ninguna fila")
    class CierreAbortado {

        @Test
        @DisplayName("cerrar una ficha que ya no es la vigente devuelve cero y no toca la fila")
        void cerrar_una_ficha_ya_cerrada_devuelve_cero() {
            // Otra sucesion gano la carrera: la ficha se cerro ayer y esta transaccion
            // llega con la foto anterior, creyendola vigente.
            CompanyTaxProfile vigente = guardarPerfilPropio();
            vigente.closeOn(CREADO.toLocalDate().plusDays(1));
            assertThat(repository.close(vigente)).as("el primer cierre si afecta la fila").isOne();

            CompanyTaxProfile mismaFichaOtraVez = perfilCerrado(vigente.getId(), CLINICA,
                    CREADO.toLocalDate().plusDays(2));

            assertThat(repository.close(mismaFichaOtraVez))
                    .as("segundo cierre sobre una ficha que ya no es la vigente").isZero();
            // Y el valid_to sigue siendo el del primer cierre: el segundo intento no
            // reescribio la fecha con la que se explica un documento de ese intervalo.
            assertThat(validToEnLaBase(vigente.getId()))
                    .isEqualTo(CREADO.toLocalDate().plusDays(1).toString());
        }

        @Test
        @DisplayName("cerrar la ficha de otra empresa devuelve cero y la deja vigente")
        void cerrar_la_ficha_de_otra_empresa_devuelve_cero() {
            // Aislamiento entre empresas a nivel de UPDATE (MUTACIONES_SQL_ACOTADAS_POR
            // _EMPRESA, BE-COV): el id existe y esta vigente, lo unico que no encaja es
            // la empresa. Sin el company_id del WHERE, esta llamada cerraria la ficha
            // fiscal de otra clinica y la dejaria sin identidad con la que facturar.
            CompanyTaxProfile ajena = guardarPerfilAjeno();
            releerDesdeLaBase();

            CompanyTaxProfile conLaEmpresaEquivocada = perfilCerrado(ajena.getId(), CLINICA,
                    CREADO.toLocalDate().plusDays(1));

            assertThat(repository.close(conLaEmpresaEquivocada))
                    .as("cierre pedido desde una empresa que no es la dueña").isZero();
            assertThat(validToEnLaBase(ajena.getId())).as("la ficha ajena sigue abierta").isNull();
            assertThat(repository.findCurrentByCompanyId(OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("cerrar de verdad mueve la version de la fila")
        void cerrar_mueve_la_version_de_la_fila() {
            // version = version + 1 va en el SET. Sin ese incremento, un save
            // concurrente que venga de una lectura anterior pisaria el cierre sin que
            // el candado optimista de Hibernate se entere.
            CompanyTaxProfile vigente = guardarPerfilPropio();
            long antes = versionEnLaBase(vigente.getId());

            vigente.closeOn(CREADO.toLocalDate().plusDays(1));
            assertThat(repository.close(vigente)).isOne();

            assertThat(versionEnLaBase(vigente.getId())).isEqualTo(antes + 1);
        }

        @Test
        @DisplayName("un cierre que no afecta ninguna fila tampoco mueve la version")
        void un_cierre_abortado_no_mueve_la_version() {
            CompanyTaxProfile ajena = guardarPerfilAjeno();
            long antes = versionEnLaBase(ajena.getId());

            assertThat(repository
                    .close(perfilCerrado(ajena.getId(), CLINICA, CREADO.toLocalDate().plusDays(1))))
                    .isZero();

            assertThat(versionEnLaBase(ajena.getId())).isEqualTo(antes);
        }
    }

    @Nested
    @DisplayName("existencia y unicidad por empresa")
    class Unicidad {

        @Test
        @DisplayName("existsCurrentByCompanyId distingue la empresa con perfil vigente de la que no tiene")
        void exists_distingue_la_empresa_con_perfil() {
            guardarPerfilPropio();
            releerDesdeLaBase();

            assertThat(repository.existsCurrentByCompanyId(COMPANY)).isTrue();
            assertThat(repository.existsCurrentByCompanyId(OTRA_COMPANY)).isFalse();
        }

        @Test
        @DisplayName("la base rechaza un segundo perfil para la misma empresa")
        void la_base_rechaza_un_segundo_perfil() {
            // uq_company_tax_profiles_current: la empresa puede tener historico, pero una
            // sola ficha VIGENTE. No lo sostiene un if del caso de uso, lo sostiene el
            // indice unico sobre la columna generada.
            guardarPerfilPropio();

            assertThatThrownBy(() -> guardarPerfilPropio())
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Duplicate entry");
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("el perfil de otra empresa no se devuelve al preguntar por la propia")
        void el_perfil_ajeno_no_se_devuelve_por_la_empresa_propia() {
            guardarPerfilAjeno();
            releerDesdeLaBase();

            assertThat(repository.findCurrentByCompanyId(COMPANY)).isEmpty();
            assertThat(repository.findCurrentByCompanyId(OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("cada empresa relee su propio perfil, sin mezclar refs ni responsabilidades")
        void cada_empresa_relee_su_propio_perfil() {
            guardarPerfilPropio();
            guardarPerfilAjeno();
            releerDesdeLaBase();

            CompanyTaxProfile propio = repository.findCurrentByCompanyId(COMPANY).orElseThrow();
            CompanyTaxProfile ajeno = repository.findCurrentByCompanyId(OTRA_COMPANY).orElseThrow();

            assertThat(propio.getCompany()).isEqualTo(CLINICA);
            assertThat(propio.getEconomicActivity()).isEqualTo(VETERINARIA);
            assertThat(propio.getResponsibilities())
                    .extracting(CompanyTaxProfileResponsibility::code)
                    .containsExactlyInAnyOrder("O-13", "O-15");
            assertThat(ajeno.getCompany()).isEqualTo(CLINICA_AJENA);
            assertThat(ajeno.getEconomicActivity()).isEqualTo(COMERCIO);
            assertThat(ajeno.getResponsibilities())
                    .extracting(CompanyTaxProfileResponsibility::code).containsExactly("O-23");
        }
    }
}
