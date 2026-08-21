package com.vetsoftware.app.companysettings.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.companysettings.domain.CompanySetting;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Rodaja de persistencia de los ajustes por empresa contra MySQL real.
 *
 * <p>
 * Lo que se comprueba aqui no existe en el codigo Java: la unicidad de
 * {@code (company_id, property_name)} la impone un indice de la base, y la FK a
 * {@code companies} la impone el schema real.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanySettingRepository — ajustes por empresa contra MySQL real")
class CompanySettingPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaCompanySettingRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private CompanySetting guardar(Long companyId, String propertyName, String value) {
        return repository.save(CompanySetting.create(companyId, propertyName, value));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Cuenta filas crudas. El {@code find} del adaptador devuelve un
     * {@code Optional}, asi que una segunda fila insertada por error se veria como
     * "todo bien" desde el dominio hasta que el indice unico o un
     * {@code NonUniqueResult} la delatara. Este COUNT es lo que la ve.
     */
    private long filas(Long companyId, String propertyName) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM company_settings
                WHERE company_id = :companyId
                  AND property_name = :propertyName
                """).setParameter("companyId", companyId).setParameter("propertyName", propertyName)
                .getSingleResult()).longValue();
    }

    private long versionCruda(Long companyId, String propertyName) {
        return ((Number) entityManager.createNativeQuery("""
                SELECT CAST(version AS SIGNED)
                FROM company_settings
                WHERE company_id = :companyId
                  AND property_name = :propertyName
                """).setParameter("companyId", companyId).setParameter("propertyName", propertyName)
                .getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer conserva los campos")
        void guardar_asigna_id_y_releer_conserva_los_campos() {
            CompanySetting guardado = guardar(COMPANY, "inventory.allow_negative_stock", "true");

            assertThat(guardado.getId()).isNotNull();

            CompanySetting leido = repository.find(COMPANY, "inventory.allow_negative_stock")
                    .orElseThrow();
            assertThat(leido.getValue()).isEqualTo("true");
            assertThat(leido.getCompanyId()).isEqualTo(COMPANY);
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una propiedad sin fila no devuelve nada")
        void una_propiedad_sin_fila_no_devuelve_nada() {
            assertThat(repository.find(COMPANY, "ausente")).isEmpty();
        }

        @Test
        @DisplayName("actualizar el value sobre la fila leida no crea una fila nueva")
        void actualizar_el_value_no_crea_una_fila_nueva() {
            CompanySetting guardado = guardar(COMPANY, "k", "1");

            CompanySetting recargado = repository.find(COMPANY, "k").orElseThrow();
            recargado.updateValue("2");
            repository.save(recargado);

            CompanySetting leido = repository.find(COMPANY, "k").orElseThrow();
            assertThat(leido.getId()).isEqualTo(guardado.getId());
            assertThat(leido.getValue()).isEqualTo("2");
        }
    }

    @Nested
    @DisplayName("un solo valor por empresa y propiedad")
    class Unicidad {

        @Test
        @DisplayName("insertar la misma pareja (empresa, propiedad) dos veces la rechaza la base")
        void insertar_la_misma_pareja_dos_veces_la_rechaza_la_base() {
            guardar(COMPANY, "k", "1");

            // Dos filas para (empresa, propiedad) partirian el ajuste en dos y el upsert
            // del service (find + save) no sabria cual actualizar. El indice unico es la
            // unica red que lo impide de verdad.
            assertThatThrownBy(() -> {
                guardar(COMPANY, "k", "2");
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("la misma propiedad en otra empresa es otra fila")
        void la_misma_propiedad_en_otra_empresa_es_otra_fila() {
            guardar(COMPANY, "k", "1");
            guardar(OTRA_COMPANY, "k", "2");

            assertThat(repository.find(COMPANY, "k").orElseThrow().getValue()).isEqualTo("1");
            assertThat(repository.find(OTRA_COMPANY, "k").orElseThrow().getValue()).isEqualTo("2");
        }
    }

    @Nested
    @DisplayName("listado por empresa")
    class ListadoPorEmpresa {

        @Test
        @DisplayName("findByCompany solo trae los ajustes de esa empresa")
        void find_by_company_solo_trae_los_ajustes_de_esa_empresa() {
            guardar(COMPANY, "a", "1");
            guardar(COMPANY, "b", "2");
            guardar(OTRA_COMPANY, "c", "3");

            assertThat(repository.findByCompany(COMPANY))
                    .extracting(CompanySetting::getPropertyName)
                    .containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("una empresa sin ajustes devuelve una lista vacia")
        void una_empresa_sin_ajustes_devuelve_una_lista_vacia() {
            assertThat(repository.findByCompany(OTRA_COMPANY)).isEmpty();
        }
    }

    /**
     * BE-26. Esta feature es el caso raro del repo: no hay clase {@code *JpaMapper}
     * —el mapeo es privado y estatico dentro de
     * {@link JpaCompanySettingRepository}— y {@code SetCompanySettingService} hace
     * leer-modificar-escribir sobre una entidad desligada. Es el patron exacto
     * donde una {@code version} que no viaja de vuelta al dominio convierte el
     * {@code merge} en un {@code persist} y duplica la fila en silencio. Por eso
     * las aserciones cuentan filas crudas.
     */
    @Nested
    @DisplayName("bloqueo optimista")
    class BloqueoOptimista {

        @Test
        @DisplayName("un ajuste recien creado nace con version cero")
        void un_ajuste_recien_creado_nace_con_version_cero() {
            guardar(COMPANY, "k", "1");
            releerDesdeLaBase();

            assertThat(versionCruda(COMPANY, "k")).isZero();
            assertThat(repository.find(COMPANY, "k")).map(CompanySetting::getVersion).contains(0L);
        }

        @Test
        @DisplayName("dos copias del mismo ajuste: la segunda en guardar choca por version obsoleta")
        void la_segunda_copia_choca_por_version_obsoleta() {
            guardar(COMPANY, "k", "1");
            releerDesdeLaBase();

            CompanySetting primeraCopia = repository.find(COMPANY, "k").orElseThrow();
            CompanySetting segundaCopia = repository.find(COMPANY, "k").orElseThrow();

            primeraCopia.updateValue("2");
            repository.save(primeraCopia);
            releerDesdeLaBase();

            segundaCopia.updateValue("3");

            assertThatThrownBy(() -> repository.save(segundaCopia))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("CompanySettingJpaEntity");
        }

        @Test
        @DisplayName("guardar un ajuste existente actualiza la unica fila y le sube la version")
        void guardar_un_ajuste_existente_actualiza_la_unica_fila() {
            CompanySetting creado = guardar(COMPANY, "k", "1");
            releerDesdeLaBase();

            CompanySetting leido = repository.find(COMPANY, "k").orElseThrow();
            leido.updateValue("2");
            repository.save(leido);
            releerDesdeLaBase();

            assertThat(filas(COMPANY, "k")).as("un INSERT duplicado se veria aqui").isEqualTo(1L);
            assertThat(repository.findByCompany(COMPANY)).hasSize(1);
            assertThat(versionCruda(COMPANY, "k")).isEqualTo(1L);
            assertThat(repository.find(COMPANY, "k")).map(CompanySetting::getId)
                    .contains(creado.getId());
        }

        @Test
        @DisplayName("dos ediciones encadenadas releyendo entre medias suben la version a dos")
        void dos_ediciones_encadenadas_suben_la_version_a_dos() {
            guardar(COMPANY, "k", "1");
            releerDesdeLaBase();

            CompanySetting primera = repository.find(COMPANY, "k").orElseThrow();
            primera.updateValue("2");
            repository.save(primera);
            releerDesdeLaBase();

            CompanySetting segunda = repository.find(COMPANY, "k").orElseThrow();
            segunda.updateValue("3");
            repository.save(segunda);
            releerDesdeLaBase();

            assertThat(filas(COMPANY, "k")).isEqualTo(1L);
            assertThat(versionCruda(COMPANY, "k")).isEqualTo(2L);
            assertThat(repository.find(COMPANY, "k")).map(CompanySetting::getValue).contains("3");
        }
    }
}
