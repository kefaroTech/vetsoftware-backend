package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * {@code JpaEmployeeAdminCheckPort} y {@code JpaEmployeeEmailQueryPort} contra
 * MySQL real: dos consultas nativas de una sola fila que
 * {@code PurchaseModulesServiceTest} solo ejercita con dobles. No se declaran
 * como beans —igual que {@code QuoteCatalogQueryPortsIT}— porque ambas solo
 * necesitan un {@code EntityManager}.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Puertos de empleado de quote — SQL nativo contra MySQL real")
class EmployeeAdminAndEmailQueryPortsPersistenceIT extends AbstractDataJpaTest {

    private static final Long ROL_ADMIN_ID = 9860L;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaEmployeeAdminCheckPort adminPort;
    private JpaEmployeeEmailQueryPort emailPort;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        adminPort = new JpaEmployeeAdminCheckPort(entityManager);
        emailPort = new JpaEmployeeEmailQueryPort(entityManager);
    }

    @Nested
    @DisplayName("El correo del empleado, dentro de su empresa")
    class Correo {

        @Test
        @DisplayName("resuelve el correo sembrado, y no el de otra empresa")
        void resuelve_el_correo_sembrado() {
            assertThat(emailPort.findEmail(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID))
                    .contains("ana@test.local");
            assertThat(emailPort.findEmail(SchemaSeed.EMPLOYEE_ID, SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("un empleado que no existe no tiene correo")
        void un_empleado_inexistente_no_tiene_correo() {
            assertThat(emailPort.findEmail(-1L, SchemaSeed.COMPANY_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Quién es administrador de la empresa (D-9)")
    class EsAdministrador {

        @BeforeEach
        void rolAdmin() {
            entityManager.createNativeQuery("""
                    INSERT INTO roles (id, name, code, company_id, created_date)
                    VALUES (:id, 'Administrador', 'ADMIN', :companyId, NOW())
                    """).setParameter("id", ROL_ADMIN_ID)
                    .setParameter("companyId", SchemaSeed.COMPANY_ID).executeUpdate();
            entityManager.createNativeQuery("""
                    INSERT INTO employee_roles (employee_id, role_id, created_date)
                    VALUES (:employeeId, :roleId, NOW())
                    """).setParameter("employeeId", SchemaSeed.EMPLOYEE_ID)
                    .setParameter("roleId", ROL_ADMIN_ID).executeUpdate();
            entityManager.flush();
        }

        @Test
        @DisplayName("el dueño de la cuenta, con el rol ADMIN en su empresa, sí lo es")
        void el_dueno_de_la_cuenta_si_es_administrador() {
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID))
                    .isTrue();
        }

        @Test
        @DisplayName("el mismo empleado no es administrador de otra empresa")
        void el_mismo_empleado_no_es_administrador_de_otra_empresa() {
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.EMPLOYEE_ID, SchemaSeed.OTRA_COMPANY_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("un empleado sin el rol ADMIN no es administrador")
        void un_empleado_sin_el_rol_admin_no_es_administrador() {
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.OTRO_EMPLOYEE_ID, SchemaSeed.COMPANY_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("sin empleado o sin empresa no es administrador")
        void sin_empleado_ni_empresa_no_es_administrador() {
            assertThat(adminPort.isCompanyAdmin(null, SchemaSeed.COMPANY_ID)).isFalse();
            assertThat(adminPort.isCompanyAdmin(SchemaSeed.EMPLOYEE_ID, null)).isFalse();
        }
    }
}
