package com.vetsoftware.app.membershipsubmodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.membershipsubmodule.domain.MembershipRef;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del enlace membresia-submodulo contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b>
 * <ul>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b> El
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * el {@code @SQLRestriction} esconde la fila de
 * {@code findAll}/{@code findById}.
 * {@code findDisabledIdByMembershipAndSubModule} y {@code reactivate} son
 * nativas y lo esquivan a proposito: son las que permiten reactivar en vez de
 * duplicar una combinacion.</li>
 * <li><b>El constraint unico (membership_id, sub_module_id) lo impone el
 * motor</b>, no el codigo Java: solo se ve intentando el INSERT duplicado.</li>
 * <li>Los {@code @EntityGraph} en {@code membership} y {@code subModule} son lo
 * que evita el N+1 al listar y al leer por id.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaMembershipSubModuleRepository — enlace, soft delete y reactivacion contra MySQL real")
class MembershipSubModulePersistenceIT extends AbstractDataJpaTest {

    private static final Long MEMBERSHIP_ID = SchemaSeed.MEMBERSHIP_ID;
    private static final Long MODULE_ID = 970L;
    private static final Long SUB_MODULE_ID = 980L;
    private static final Long OTRO_SUB_MODULE_ID = 981L;

    private static final MembershipRef PLAN = new MembershipRef(MEMBERSHIP_ID, "Plan test");
    private static final SubModuleRef FACTURACION = new SubModuleRef(SUB_MODULE_ID, "Facturacion",
            "FACT-IT");
    private static final SubModuleRef INVENTARIO = new SubModuleRef(OTRO_SUB_MODULE_ID,
            "Inventario", "INV-IT");

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaMembershipSubModuleRepository repository;

    /**
     * El repositorio de Spring Data, a pelo: {@code hasEnabledSubModuleCode} no
     * pasa por el adaptador de dominio y hasta la incidencia #185 nadie lo
     * ejecutaba contra un motor real.
     */
    @Autowired
    private MembershipSubModuleJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO modules (id, name, code) VALUES (:id, 'Administracion', 'ADMIN-IT')
                                """)
                .setParameter("id", MODULE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO sub_modules (id, name, code, module_id)
                VALUES (:id, 'Facturacion', 'FACT-IT', :moduleId)
                """).setParameter("id", SUB_MODULE_ID).setParameter("moduleId", MODULE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO sub_modules (id, name, code, module_id)
                VALUES (:id, 'Inventario', 'INV-IT', :moduleId)
                """).setParameter("id", OTRO_SUB_MODULE_ID).setParameter("moduleId", MODULE_ID)
                .executeUpdate();
        entityManager.flush();
    }

    private MembershipSubModule nueva(SubModuleRef subModulo) {
        return new MembershipSubModule(null, PLAN, subModulo, CREADO, true);
    }

    private MembershipSubModule guardar(SubModuleRef subModulo) {
        return repository.save(nueva(subModulo));
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ida y vuelta del enlace")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva membership, subModule y la fecha de creacion")
        void guardar_y_releer_conserva_cada_campo() {
            MembershipSubModule guardado = guardar(FACTURACION);

            MembershipSubModule leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getMembership()).isEqualTo(PLAN);
            assertThat(leido.getSubModule()).isEqualTo(FACTURACION);
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el listado trae los enlaces con sus referencias hidratadas")
        void el_listado_trae_los_enlaces_hidratados() {
            MembershipSubModule guardado = guardar(FACTURACION);

            assertThat(repository.findAll()).extracting(MembershipSubModule::getId)
                    .contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("constraint unico")
    class ConstraintUnico {

        @Test
        @DisplayName("el motor impide dos filas activas para la misma membresia y submodulo")
        void el_motor_impide_dos_filas_activas_para_la_misma_combinacion() {
            guardar(FACTURACION);

            assertThatThrownBy(() -> repository.save(nueva(FACTURACION)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDeleteYReactivacion {

        @Test
        @DisplayName("el listado activo deja de ver el enlace deshabilitado")
        void el_listado_activo_no_ve_el_deshabilitado() {
            MembershipSubModule guardado = guardar(FACTURACION);

            deshabilitar(guardado.getId());

            assertThat(repository.findAll()).extracting(MembershipSubModule::getId)
                    .doesNotContain(guardado.getId());
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivar devuelve el enlace al listado activo")
        void reactivar_devuelve_el_enlace_al_listado_activo() {
            MembershipSubModule guardado = guardar(FACTURACION);
            deshabilitar(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivar un id inexistente no toca ninguna fila")
        void reactivar_un_id_inexistente_no_toca_ninguna_fila() {
            int filas = repository.reactivate(999999L);

            assertThat(filas).isZero();
        }

        @Test
        @DisplayName("findDisabledIdByMembershipAndSubModule esquiva el SQLRestriction y ve el deshabilitado")
        void find_disabled_id_esquiva_el_sql_restriction() {
            MembershipSubModule guardado = guardar(INVENTARIO);
            deshabilitar(guardado.getId());

            Optional<Long> encontrado = repository
                    .findDisabledIdByMembershipAndSubModule(MEMBERSHIP_ID, OTRO_SUB_MODULE_ID);

            assertThat(encontrado).contains(guardado.getId());
        }

        @Test
        @DisplayName("una combinacion activa no aparece como deshabilitada")
        void una_combinacion_activa_no_aparece_como_deshabilitada() {
            guardar(FACTURACION);

            Optional<Long> encontrado = repository
                    .findDisabledIdByMembershipAndSubModule(MEMBERSHIP_ID, SUB_MODULE_ID);

            assertThat(encontrado).isEmpty();
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("delete es un soft delete: la fila sigue siendo localizable por la via nativa")
        void delete_es_un_soft_delete() {
            MembershipSubModule guardado = guardar(FACTURACION);

            deshabilitar(guardado.getId());

            assertThat(
                    repository.findDisabledIdByMembershipAndSubModule(MEMBERSHIP_ID, SUB_MODULE_ID))
                    .contains(guardado.getId());
        }
    }

    /**
     * <b>El gate de facturacion electronica, ejecutado contra el motor.</b>
     *
     * <p>
     * {@code hasEnabledSubModuleCode} es el cuerpo entero de
     * {@code JpaBillingEntitlementQueryPort.isElectronicInvoicingEnabled}, y esa es
     * la <b>primera</b> lectura a base de datos de toda emision, transmision,
     * reconciliacion y webhook DIAN. Estuvo caida al 100 % (#185) porque su JPQL
     * decia {@code SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END} y con
     * Hibernate 7 esa expresion se tipa {@code Integer}, asi que la coercion del
     * literal booleano lanzaba
     * {@code Cannot coerce value 'true' [java.lang.Boolean] to Integer}.
     *
     * <p>
     * <b>Ningun doble puede detectar eso</b>: el defecto vive en la traduccion de
     * la consulta y en la extraccion del resultado, no en el codigo Java. El unico
     * uso que tenia en {@code src/test} era un mock, y por eso el fallo llego a
     * produccion. Estos casos existen para que la consulta se ejecute de verdad.
     */
    @Nested
    @DisplayName("hasEnabledSubModuleCode — el gate de facturacion electronica (#185)")
    class GateDeFacturacionElectronica {

        private static final String CODIGO_FACTURACION = "FACT-IT";
        private static final String CODIGO_INVENTARIO = "INV-IT";

        private void deshabilitarSubModulo(Long subModuleId) {
            entityManager.createNativeQuery("""
                    UPDATE sub_modules SET enabled = false WHERE id = :id
                    """).setParameter("id", subModuleId).executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("con el enlace y el submodulo activos devuelve true sin reventar en la coercion")
        void con_el_enlace_activo_devuelve_true() {
            guardar(FACTURACION);

            // Este es el caso exacto que lanzaba Cannot coerce value 'true' a Integer:
            // el resultado positivo, que es el que habilita la facturacion.
            assertThat(jpaRepository.hasEnabledSubModuleCode(MEMBERSHIP_ID, CODIGO_FACTURACION))
                    .isTrue();
        }

        @Test
        @DisplayName("sin enlace para ese codigo devuelve false")
        void sin_enlace_para_ese_codigo_devuelve_false() {
            guardar(FACTURACION);

            assertThat(jpaRepository.hasEnabledSubModuleCode(MEMBERSHIP_ID, CODIGO_INVENTARIO))
                    .isFalse();
        }

        @Test
        @DisplayName("un codigo que no existe en ningun submodulo devuelve false")
        void un_codigo_inexistente_devuelve_false() {
            guardar(FACTURACION);

            assertThat(jpaRepository.hasEnabledSubModuleCode(MEMBERSHIP_ID, "NO-EXISTE")).isFalse();
        }

        @Test
        @DisplayName("con el enlace deshabilitado (soft delete) devuelve false")
        void con_el_enlace_deshabilitado_devuelve_false() {
            MembershipSubModule guardado = guardar(FACTURACION);

            deshabilitar(guardado.getId());

            assertThat(jpaRepository.hasEnabledSubModuleCode(MEMBERSHIP_ID, CODIGO_FACTURACION))
                    .isFalse();
        }

        @Test
        @DisplayName("con el submodulo deshabilitado devuelve false aunque el enlace siga activo")
        void con_el_submodulo_deshabilitado_devuelve_false() {
            guardar(INVENTARIO);

            deshabilitarSubModulo(OTRO_SUB_MODULE_ID);

            assertThat(jpaRepository.hasEnabledSubModuleCode(MEMBERSHIP_ID, CODIGO_INVENTARIO))
                    .isFalse();
        }

        @Test
        @DisplayName("otra membresia no hereda el submodulo de esta")
        void otra_membresia_no_hereda_el_submodulo() {
            guardar(FACTURACION);

            assertThat(jpaRepository.hasEnabledSubModuleCode(999999L, CODIGO_FACTURACION))
                    .isFalse();
        }
    }
}
