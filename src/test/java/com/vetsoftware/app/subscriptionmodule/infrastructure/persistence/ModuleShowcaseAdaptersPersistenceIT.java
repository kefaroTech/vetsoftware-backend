package com.vetsoftware.app.subscriptionmodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyModuleLineQueryPort.CompanyModuleLine;
import com.vetsoftware.app.subscriptionmodule.application.port.out.CompanyUsageSnapshotQueryPort.CapacitySnapshot;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Tres de los cuatro adaptadores de {@code JpaModuleShowcaseAdapters} contra
 * MySQL real: SQL nativo con acceso posicional al resultado, que hasta ahora
 * solo ejercitaba {@code ListModuleShowcaseServiceTest} con los puertos
 * mockeados. Fuera de esta rodaja, a propósito y por el mismo motivo que
 * {@code QuoteCatalogQueryPortsIT}: {@code listActiveModules}, que depende de
 * qué tarifa resuelve como «la vigente hoy» entre la de laboratorio sembrada
 * por Liquibase y {@code LISTA-TEST} de {@link SchemaSeed} — ambigüedad que no
 * aporta nada nuevo sobre lo que ya cubre
 * {@code SubscriptionOutboundPortsPersistenceIT} para esa misma resolución.
 *
 * <p>
 * <b>No se declaran como beans.</b> Los cuatro solo necesitan un
 * {@code EntityManager} (uno de ellos, además, un {@code Clock}), así que se
 * construyen a mano: añadirlos al {@code @Import} de la rodaja cambiaría la
 * clave del {@code MergedContextConfiguration} y costaría un arranque de
 * contexto entero.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaModuleShowcaseAdapters — el escaparate contra MySQL real")
class ModuleShowcaseAdaptersPersistenceIT extends AbstractDataJpaTest {

    private static final Long ROL_ADMIN_ID = 9850L;
    private static final Long ROL_ADMIN_OTRA_EMPRESA_ID = 9851L;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaModuleShowcaseAdapters.JpaCompanyModuleLineQueryPort lineaPort;
    private JpaModuleShowcaseAdapters.JpaCompanyUsageSnapshotQueryPort usoPort;
    private JpaModuleShowcaseAdapters.JpaEmployeeAdminCheckPort adminPort;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
        lineaPort = new JpaModuleShowcaseAdapters.JpaCompanyModuleLineQueryPort(entityManager);
        usoPort = new JpaModuleShowcaseAdapters.JpaCompanyUsageSnapshotQueryPort(entityManager,
                Clock.fixed(Instant.parse("2026-01-15T10:15:30Z"), ZoneOffset.UTC));
        adminPort = new JpaModuleShowcaseAdapters.JpaEmployeeAdminCheckPort(entityManager);
    }

    @Nested
    @DisplayName("La línea vigente de un artículo, por empresa")
    class LineaVigente {

        @Test
        @DisplayName("cada empresa ve su propia línea del núcleo, con su modo de cobro")
        void cada_empresa_ve_su_propia_linea_del_nucleo() {
            assertThat(lineaPort.findCurrentLine(SchemaSeed.COMPANY_ID, nucleo))
                    .contains(new CompanyModuleLine("PAID", null));
            assertThat(lineaPort.findCurrentLine(SchemaSeed.OTRA_COMPANY_ID, nucleo))
                    .contains(new CompanyModuleLine("PAID", null));
        }

        @Test
        @DisplayName("un artículo que la empresa no tiene contratado no tiene línea")
        void un_articulo_no_contratado_no_tiene_linea() {
            assertThat(lineaPort.findCurrentLine(SchemaSeed.COMPANY_ID, -1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("La capacidad usada de un eje")
    class Capacidad {

        @Test
        @DisplayName("lee el tope y lo usado del eje USER sembrado, y no el de otra empresa")
        void lee_el_tope_y_lo_usado_del_eje_sembrado() {
            assertThat(usoPort.findCapacity(SchemaSeed.COMPANY_ID, "USER"))
                    .contains(new CapacitySnapshot(2, 0));
            assertThat(usoPort.findCapacity(SchemaSeed.OTRA_COMPANY_ID, "USER")).isEmpty();
        }

        @Test
        @DisplayName("un eje sin capacidad registrada vuelve vacío")
        void un_eje_sin_capacidad_registrada_vuelve_vacio() {
            assertThat(usoPort.findCapacity(SchemaSeed.COMPANY_ID, "BRANCH")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Quién es administrador de la empresa (D-9)")
    class EsAdministrador {

        @BeforeEach
        void rolAdmin() {
            insertarRolAdmin(ROL_ADMIN_ID, SchemaSeed.COMPANY_ID, SchemaSeed.EMPLOYEE_ID);
            insertarRolAdmin(ROL_ADMIN_OTRA_EMPRESA_ID, SchemaSeed.OTRA_COMPANY_ID,
                    SchemaSeed.OTRO_EMPLOYEE_ID);
        }

        private void insertarRolAdmin(Long rolId, Long companyId, Long employeeId) {
            entityManager.createNativeQuery("""
                    INSERT INTO roles (id, name, code, company_id, created_date)
                    VALUES (:id, 'Administrador', 'ADMIN', :companyId, NOW())
                    """).setParameter("id", rolId).setParameter("companyId", companyId)
                    .executeUpdate();
            entityManager.createNativeQuery("""
                    INSERT INTO employee_roles (employee_id, role_id, created_date)
                    VALUES (:employeeId, :roleId, NOW())
                    """).setParameter("employeeId", employeeId).setParameter("roleId", rolId)
                    .executeUpdate();
            entityManager.flush();
        }

        @Test
        @DisplayName("el dueño de la cuenta, con el rol ADMIN en su empresa, sí lo es")
        void el_dueno_de_la_cuenta_si_es_administrador() {
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID))
                    .isTrue();
        }

        @Test
        @DisplayName("el ADMIN de otra empresa no cuenta como administrador de esta")
        void el_admin_de_otra_empresa_no_cuenta_aqui() {
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.OTRO_EMPLOYEE_ID, SchemaSeed.COMPANY_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("sin empleado o sin empresa no es administrador, y no se consulta la base")
        void sin_empleado_ni_empresa_no_es_administrador() {
            assertThat(adminPort.isCompanyAdmin(null, SchemaSeed.COMPANY_ID)).isFalse();
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.EMPLOYEE_ID, null)).isFalse();
        }
    }
}
