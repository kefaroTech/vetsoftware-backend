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
 * <li>El {@code company_id} es UNIQUE y la unicidad <em>no</em> mira
 * {@code enabled}: quien ya tiene perfil dado de baja no puede crear otro,
 * tiene que reactivarlo. Eso lo decide el indice, no el caso de uso.
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
                CompanyTaxProfileMother.NOMBRE_COMERCIAL, actividad, responsabilidades, CREADO,
                null, true);
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

            CompanyTaxProfile leido = repository.findByCompanyId(COMPANY).orElseThrow();

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

            CompanyTaxProfile leido = repository.findByCompanyId(COMPANY).orElseThrow();

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

            assertThat(repository.findByCompanyId(COMPANY)).map(CompanyTaxProfile::getTaxRegime)
                    .contains(regimen);
        }

        @Test
        @DisplayName("editar el perfil deja vigente solo la responsabilidad nueva")
        void editar_deja_vigente_solo_la_responsabilidad_nueva() {
            // El orphanRemoval que sostiene el bug del borrado es tambien lo que sostiene
            // la edicion: quitar una responsabilidad tiene que retirarla del perfil. Por
            // eso el arreglo saco la baja logica de JPA en vez de quitar el orphanRemoval.
            // Y como la fila hija tiene su propio @SQLDelete, el orphanRemoval no la
            // arranca: la marca enabled = false y el historico fiscal queda entero.
            Long id = guardarPerfilPropio().getId();
            releerDesdeLaBase();
            CompanyTaxProfile aEditar = repository.findByCompanyId(COMPANY).orElseThrow();

            aEditar.update(CompanyDocumentType.NIT, CompanyTaxProfileMother.NIT,
                    CompanyTaxProfileMother.NIT_DV, "Clinica Veterinaria Sur S.A.S.",
                    TaxRegime.NO_RESPONSABLE_IVA, "contabilidad@vetsur.com", "Vet Sur", COMERCIO,
                    List.of(O23));
            repository.save(aEditar);
            releerDesdeLaBase();

            CompanyTaxProfile leido = repository.findByCompanyId(COMPANY).orElseThrow();
            assertThat(leido.getId()).isEqualTo(id);
            assertThat(responsabilidadesVigentesEnLaBase(id)).isEqualTo(1L);
            assertThat(responsabilidadesEnLaBase(id)).isEqualTo(2 + 1L);
            assertThat(leido.getResponsibilities())
                    .extracting(CompanyTaxProfileResponsibility::code).containsExactly("O-23");
            assertThat(leido.getLegalName()).isEqualTo("Clinica Veterinaria Sur S.A.S.");
            assertThat(leido.getEconomicActivity()).isEqualTo(COMERCIO);
        }
    }

    @Nested
    @DisplayName("existencia y unicidad por empresa")
    class Unicidad {

        @Test
        @DisplayName("existsByCompanyId distingue la empresa con perfil de la que no tiene")
        void exists_distingue_la_empresa_con_perfil() {
            guardarPerfilPropio();
            releerDesdeLaBase();

            assertThat(repository.existsByCompanyId(COMPANY)).isTrue();
            assertThat(repository.existsByCompanyId(OTRA_COMPANY)).isFalse();
        }

        @Test
        @DisplayName("la base rechaza un segundo perfil para la misma empresa")
        void la_base_rechaza_un_segundo_perfil() {
            // company_id es UNIQUE: la relacion es 0-o-1 y no la sostiene un if del caso
            // de uso, la sostiene el indice.
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

            assertThat(repository.findByCompanyId(COMPANY)).isEmpty();
            assertThat(repository.findByCompanyId(OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("cada empresa relee su propio perfil, sin mezclar refs ni responsabilidades")
        void cada_empresa_relee_su_propio_perfil() {
            guardarPerfilPropio();
            guardarPerfilAjeno();
            releerDesdeLaBase();

            CompanyTaxProfile propio = repository.findByCompanyId(COMPANY).orElseThrow();
            CompanyTaxProfile ajeno = repository.findByCompanyId(OTRA_COMPANY).orElseThrow();

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
