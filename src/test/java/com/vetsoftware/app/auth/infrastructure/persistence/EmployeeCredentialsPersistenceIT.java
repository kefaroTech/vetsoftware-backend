package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del inicio de sesion de un empleado contra MySQL real.
 *
 * <p>
 * El servicio de login solo compara hashes: quien decide si la cuenta existe —y
 * con que empresa, que {@code authVersion} y que estado de verificacion de
 * correo— es esta consulta. Y el detalle que la hace peligrosa esta en la base,
 * no en el codigo: <b>{@code employee_code} es unico GLOBALMENTE</b>, sin
 * {@code company_id}, mientras que la entidad lleva
 * {@code @SQLRestriction("enabled = true")}.
 *
 * <p>
 * Las dos cosas juntas producen el patron de «borrado logico con clave unica»:
 * un empleado dado de baja <b>desaparece de la busqueda pero sigue ocupando su
 * codigo</b>. Nadie que lea el adaptador puede deducirlo, y es la causa de
 * varios defectos de esta campaña: la pantalla de alta cree que el codigo esta
 * libre —lo consulta por el camino que filtra— y el {@code INSERT} choca contra
 * un indice que no ve.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaEmployeeCredentialsRepository — credenciales por código contra MySQL real")
class EmployeeCredentialsPersistenceIT extends AbstractDataJpaTest {

    private static final String CODIGO = "EMP-001";
    private static final String HASH = "$2a$12$abcdefghijklmnopqrstuv.wxyz0123456789ABCDEFGHIJKLMN";

    @Autowired
    private JpaEmployeeCredentialsRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    private void vaciarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    /** Deja la fila del empleado sembrado en un estado explicito y afirmable. */
    private void fijarCredenciales(Long employeeId, String hash, long authVersion,
            boolean emailVerified) {
        entityManager
                .createNativeQuery("UPDATE employees SET hash_password = :hash,"
                        + " auth_version = :auth, email_verified = :verificado WHERE id = :id")
                .setParameter("hash", hash).setParameter("auth", authVersion)
                .setParameter("verificado", emailVerified).setParameter("id", employeeId)
                .executeUpdate();
        vaciarContexto();
    }

    private void desactivar(Long employeeId) {
        entityManager.createNativeQuery("UPDATE employees SET enabled = false WHERE id = :id")
                .setParameter("id", employeeId).executeUpdate();
        vaciarContexto();
    }

    private void insertarEmpleado(Long id, String codigo, Long companyId) {
        entityManager
                .createNativeQuery("INSERT INTO employees (id, employee_code, hash_password,"
                        + " name, email, company_id) VALUES (:id, :codigo, 'x', 'Suplantador',"
                        + " :correo, :empresa)")
                .setParameter("id", id).setParameter("codigo", codigo)
                .setParameter("correo", "empleado" + id + "@test.local")
                .setParameter("empresa", companyId).executeUpdate();
        vaciarContexto();
    }

    @Nested
    @DisplayName("Lectura de credenciales")
    class Lectura {

        @Test
        @DisplayName("devuelve los cinco campos tal como están en la fila, con la empresa hidratada")
        void devuelve_los_cinco_campos_de_la_fila() {
            // La empresa llega por el @EntityGraph("company"): el adaptador hace
            // e.getCompany().getId() y sin el grafo eso seria una segunda consulta —o un
            // LazyInitializationException fuera de transaccion—.
            fijarCredenciales(SchemaSeed.EMPLOYEE_ID, HASH, 9L, true);

            assertThat(repository.findByCode(CODIGO)).get().satisfies(credenciales -> {
                assertThat(credenciales.id()).isEqualTo(SchemaSeed.EMPLOYEE_ID);
                assertThat(credenciales.companyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(credenciales.authVersion()).isEqualTo(9L);
                assertThat(credenciales.hashPassword()).isEqualTo(HASH);
                assertThat(credenciales.emailVerified()).isTrue();
            });
        }

        @Test
        @DisplayName("un correo sin verificar viaja como false: el gate del login lo necesita")
        void un_correo_sin_verificar_viaja_como_false() {
            fijarCredenciales(SchemaSeed.EMPLOYEE_ID, HASH, 0L, false);

            assertThat(repository.findByCode(CODIGO)).get()
                    .satisfies(credenciales -> assertThat(credenciales.emailVerified()).isFalse());
        }

        @Test
        @DisplayName("la búsqueda es global, no por empresa: encuentra al empleado de otra clínica")
        void la_busqueda_es_global_no_por_empresa() {
            // Es lo correcto y es deliberado: en el login todavia no hay tenant, el
            // codigo ES el identificador global. Queda fijado para que nadie "arregle"
            // esta consulta acotandola por empresa y deje sin entrar a media plataforma.
            insertarEmpleado(942L, "EMP-AJENO", SchemaSeed.OTRA_COMPANY_ID);

            assertThat(repository.findByCode("EMP-AJENO")).get()
                    .satisfies(credenciales -> assertThat(credenciales.companyId())
                            .isEqualTo(SchemaSeed.OTRA_COMPANY_ID));
        }

        @Test
        @DisplayName("un código que no existe devuelve vacío")
        void un_codigo_que_no_existe_devuelve_vacio() {
            assertThat(repository.findByCode("NO-EXISTE")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Borrado lógico con clave única global")
    class BorradoLogicoYCodigoUnico {

        @Test
        @DisplayName("un empleado dado de baja desaparece de la búsqueda de credenciales")
        void un_empleado_dado_de_baja_desaparece_de_la_busqueda() {
            desactivar(SchemaSeed.EMPLOYEE_ID);

            assertThat(repository.findByCode(CODIGO)).isEmpty();
        }

        @Test
        @DisplayName("pero SIGUE ocupando su código: la base rechaza reutilizarlo")
        void pero_sigue_ocupando_su_codigo() {
            // Las dos mitades del patron. El @SQLRestriction esconde la fila de JPA; el
            // indice unico de la base la cuenta igual. Quien mire solo por el camino
            // filtrado creera que el codigo esta libre y el INSERT reventara despues,
            // lejos de aqui y disfrazado de otra cosa.
            desactivar(SchemaSeed.EMPLOYEE_ID);
            assertThat(repository.findByCode(CODIGO)).isEmpty();

            assertThatThrownBy(() -> insertarEmpleado(943L, CODIGO, SchemaSeed.COMPANY_ID))
                    .hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class)
                    .rootCause().hasMessageContaining(CODIGO);
        }
    }
}
