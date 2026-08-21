package com.vetsoftware.app.permission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.permission.domain.CompanyRef;
import com.vetsoftware.app.permission.domain.Permission;
import com.vetsoftware.app.permission.domain.SubModuleRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del permiso contra MySQL real.
 *
 * <p>
 * {@code PermissionJpaEntity} tiene FKs obligatorias a {@code companies} y
 * {@code sub_modules}, y esta ultima a {@code modules}. {@link SchemaSeed} no
 * siembra ese catalogo, asi que este test completa la cadena con sus propias
 * filas (ids 970+, fuera del rango de {@link SchemaSeed}).
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPermissionRepository — permisos contra MySQL real")
class PermissionPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long MODULE_ID = 970L;
    private static final Long SUB_MODULE_ID = 971L;

    private static final CompanyRef CLINICA = new CompanyRef(EMPRESA, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef OTRA_CLINICA = new CompanyRef(OTRA_EMPRESA, "Veterinaria ajena",
            "900654321");
    private static final SubModuleRef INVENTARIO = new SubModuleRef(SUB_MODULE_ID, "Inventario-PT",
            "INV-PT");

    @Autowired
    private JpaPermissionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO modules (id, name, code, created_date, enabled)
                VALUES (:id, 'Inventario-PT-mod', 'INV-PT-MOD', '2026-01-01 00:00:00', true)
                """).setParameter("id", MODULE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO sub_modules (id, name, code, module_id, created_date, enabled)
                VALUES (:id, 'Inventario-PT', 'INV-PT', :modulo, '2026-01-01 00:00:00', true)
                """).setParameter("id", SUB_MODULE_ID).setParameter("modulo", MODULE_ID)
                .executeUpdate();
        entityManager.flush();
    }

    private Permission nuevoPermiso(String name, String code, CompanyRef company) {
        return Permission.create(name, code, company, INVENTARIO);
    }

    @Nested
    @DisplayName("guardar y releer")
    class GuardarYReleer {

        @Test
        @DisplayName("guarda el permiso y lo relee con empresa y submodulo resueltos")
        void guarda_el_permiso_y_lo_relee() {
            Permission guardado = repository
                    .save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            entityManager.clear();

            Optional<Permission> releido = repository.findById(guardado.getId());

            assertThat(releido).isPresent();
            assertThat(releido.get().getCompany()).isEqualTo(CLINICA);
            assertThat(releido.get().getSubModule()).isEqualTo(INVENTARIO);
            assertThat(releido.get().getName()).isEqualTo("Crear factura");
            assertThat(releido.get().getCode()).isEqualTo("billing.create");
            assertThat(releido.get().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un id inexistente no devuelve nada")
        void un_id_inexistente_no_devuelve_nada() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado por empresa")
    class ListadoPorEmpresa {

        @Test
        @DisplayName("findAllByCompanyId solo devuelve los de esa empresa")
        void find_all_by_company_id_solo_devuelve_los_de_esa_empresa() {
            repository.save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            repository.save(nuevoPermiso("Ver reportes", "reports.read", OTRA_CLINICA));
            entityManager.flush();
            entityManager.clear();

            List<Permission> deLaEmpresa = repository.findAllByCompanyId(EMPRESA);

            assertThat(deLaEmpresa).extracting(Permission::getCode)
                    .containsExactly("billing.create");
        }

        @Test
        @DisplayName("findAll devuelve los de todas las empresas")
        void find_all_devuelve_los_de_todas_las_empresas() {
            repository.save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("borrado logico")
    class BorradoLogico {

        @Test
        @DisplayName("borrar deshabilita la fila: deja de aparecer en las lecturas")
        void borrar_deshabilita_la_fila() {
            Permission guardado = repository
                    .save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            entityManager.clear();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactivate vuelve a habilitar una fila deshabilitada")
        void reactivate_vuelve_a_habilitar_una_fila_deshabilitada() {
            Permission guardado = repository
                    .save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId());
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta ninguna fila")
        void reactivar_un_id_inexistente_no_afecta_ninguna_fila() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /** Lee el flag real de la fila, tambien cuando el @SQLRestriction la oculta. */
        private boolean estaHabilitado(Long id) {
            return ((Number) entityManager
                    .createNativeQuery("SELECT enabled FROM permissions WHERE id = :id")
                    .setParameter("id", id).getSingleResult()).intValue() == 1;
        }

        @Test
        @DisplayName("findByIdAndCompanyId no ve el permiso desde otra empresa")
        void find_by_id_and_company_id_no_ve_el_de_otra_empresa() {
            Permission guardado = repository
                    .save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), EMPRESA)).isPresent();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_EMPRESA)).isEmpty();
        }

        @Test
        @DisplayName("reactivate acotado a su empresa vuelve a habilitar la fila")
        void reactivate_acotado_a_su_empresa_reactiva() {
            Permission guardado = repository
                    .save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId(), EMPRESA);
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate con el companyId de otra empresa afecta 0 filas y lo deja deshabilitado")
        void reactivate_desde_otra_empresa_no_afecta_ninguna_fila() {
            Permission guardado = repository
                    .save(nuevoPermiso("Crear factura", "billing.create", CLINICA));
            entityManager.flush();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId(), OTRA_EMPRESA);
            entityManager.clear();

            assertThat(filas).isZero();
            assertThat(estaHabilitado(guardado.getId())).isFalse();
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }
}
