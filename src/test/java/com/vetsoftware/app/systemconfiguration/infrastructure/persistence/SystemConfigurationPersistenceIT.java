package com.vetsoftware.app.systemconfiguration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la configuracion global contra MySQL real.
 *
 * <p>
 * A diferencia de {@code company_settings}, esta tabla no tiene FK: no hace
 * falta {@code SchemaSeed}. Lo que se comprueba aqui y no existe en el codigo
 * Java: la unicidad de {@code property_name} la impone un indice de la base, y
 * el filtro de filas deshabilitadas lo impone el {@code @SQLRestriction} de
 * Hibernate, no una condicion en el repositorio.
 *
 * <p>
 * <b>Ninguna propiedad de test se llama {@code "uvt"}.</b> La migracion
 * {@code 148_create_system_configurations} siembra una fila real con
 * {@code property_name='uvt'} en cualquier base migrada desde cero — esta clase
 * corre contra Liquibase real, no contra una base vacia. Un test que insertara
 * {@code "uvt"} chocaria con esa fila y fallaria por una
 * {@code DataIntegrityViolationException} que no tiene nada que ver con lo que
 * el test dice probar. Descubierto escribiendo esta rodaja: no hay Clock ni
 * Random involucrados, es puramente el seed de produccion.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSystemConfigurationRepository — configuracion global contra MySQL real")
class SystemConfigurationPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSystemConfigurationRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private SystemConfiguration guardar(String propertyName, String value) {
        return repository.save(SystemConfiguration.create(propertyName, value));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer conserva los campos")
        void guardar_asigna_id_y_releer_conserva_los_campos() {
            SystemConfiguration guardado = guardar("test.threshold", "47065");

            assertThat(guardado.getId()).isNotNull();

            SystemConfiguration leido = repository.findByPropertyName("test.threshold")
                    .orElseThrow();
            assertThat(leido.getValue()).isEqualTo("47065");
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("una propiedad sin fila no devuelve nada")
        void una_propiedad_sin_fila_no_devuelve_nada() {
            assertThat(repository.findByPropertyName("ausente")).isEmpty();
        }
    }

    @Nested
    @DisplayName("una sola fila por propiedad")
    class Unicidad {

        @Test
        @DisplayName("insertar la misma propiedad dos veces la rechaza la base")
        void insertar_la_misma_propiedad_dos_veces_la_rechaza_la_base() {
            guardar("test.threshold", "47065");

            assertThatThrownBy(() -> {
                guardar("test.threshold", "48000");
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("listado completo")
    class Listado {

        @Test
        @DisplayName("findAll trae todas las configuraciones habilitadas, incluida la sembrada por Liquibase")
        void find_all_trae_todas_las_configuraciones_habilitadas() {
            guardar("test.threshold", "47065");
            guardar("test.other", "5");

            // La migracion 148 siembra una fila 'uvt' en cualquier base migrada desde
            // cero: findAll la trae tambien. `contains` en vez de exact match porque esa
            // fila no la crea este test.
            assertThat(repository.findAll()).extracting(SystemConfiguration::getPropertyName)
                    .contains("test.threshold", "test.other", "uvt");
        }

        @Test
        @DisplayName("el SQLRestriction excluye las filas deshabilitadas del listado y de la busqueda")
        void el_sql_restriction_excluye_las_filas_deshabilitadas() {
            // @SQLDelete convierte cualquier borrado en un UPDATE enabled=false, pero el
            // puerto no expone delete, asi que se simula insertando la fila ya
            // deshabilitada por SQL nativo. Lo que se comprueba es el @SQLRestriction:
            // que Hibernate filtra enabled=true tanto en findAll como en el finder.
            entityManager.createNativeQuery("""
                    INSERT INTO system_configurations (property_name, value, created_date, enabled)
                    VALUES ('desactivada', '0', '2026-01-15 08:00:00', false)
                    """).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll()).extracting(SystemConfiguration::getPropertyName)
                    .doesNotContain("desactivada");
            assertThat(repository.findByPropertyName("desactivada")).isEmpty();
        }
    }
}
