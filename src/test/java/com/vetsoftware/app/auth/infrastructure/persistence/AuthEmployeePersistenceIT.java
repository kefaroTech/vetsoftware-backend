package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.auth.application.port.out.AuthEmployeeRepository.AuthEmployee;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del estado autenticable de un empleado contra MySQL
 * real.
 *
 * <p>
 * Este adaptador es de tres lineas y no tiene nada que probar: todo lo que
 * decide si un empleado puede seguir dentro vive en SQL que nadie ejercitaba.
 *
 * <ul>
 * <li><b>Las tres puertas de {@code findActiveWithCompanyById}.</b> El JPQL
 * exige {@code e.enabled = true} <em>y</em> {@code c.enabled = true} sobre el
 * {@code JOIN FETCH} de la empresa. La segunda es la que nadie mira: suspender
 * una clinica morosa tiene que dejar fuera a su personal, y eso solo lo dice
 * esa condicion.
 * <li><b>La rotacion bajo bloqueo pesimista.</b> El valor que se devuelve en el
 * record y el que queda en la fila tienen que ser el mismo numero; si se
 * separan, el JWT recien emitido nace invalido y el empleado no entra.
 * <li><b>El {@code AND company_id} de la sobrecarga acotada de
 * {@code bumpAuthVersion}.</b> Es la unica barrera del logout —su puerto es
 * {@code isAuthenticated()}, el gate mas debil del proyecto— y es SQL nativo,
 * asi que un doble del repositorio no puede falsearla: responderia lo que el
 * test le hubiera dicho.
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAuthEmployeeRepository — estado autenticable del empleado contra MySQL real")
class AuthEmployeePersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_EMPLEADO = SchemaSeed.OTRO_EMPLOYEE_ID;
    private static final Long LA_EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long LA_EMPRESA_AJENA = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaAuthEmployeeRepository repository;

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

    /** Deja la fila en un estado conocido: sin esto, los defaults del schema. */
    private void fijarVersiones(Long employeeId, long authVersion, long version) {
        entityManager
                .createNativeQuery("UPDATE employees SET auth_version = :auth, version = :ver"
                        + " WHERE id = :id")
                .setParameter("auth", authVersion).setParameter("ver", version)
                .setParameter("id", employeeId).executeUpdate();
        vaciarContexto();
    }

    private void desactivarEmpleado(Long employeeId) {
        entityManager.createNativeQuery("UPDATE employees SET enabled = false WHERE id = :id")
                .setParameter("id", employeeId).executeUpdate();
        vaciarContexto();
    }

    private void desactivarEmpresa(Long companyId) {
        entityManager.createNativeQuery("UPDATE companies SET enabled = false WHERE id = :id")
                .setParameter("id", companyId).executeUpdate();
        vaciarContexto();
    }

    private long columna(String columna, Long employeeId) {
        return ((Number) entityManager
                .createNativeQuery("SELECT " + columna + " FROM employees WHERE id = :id")
                .setParameter("id", employeeId).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("Lectura del empleado activo — las tres puertas del JOIN FETCH")
    class LecturaDelEmpleadoActivo {

        @Test
        @DisplayName("con empleado y empresa activos devuelve el id, su empresa y su authVersion")
        void con_empleado_y_empresa_activos_devuelve_el_estado_completo() {
            fijarVersiones(EMPLEADO, 7L, 3L);

            assertThat(repository.findActiveById(EMPLEADO)).get().satisfies(auth -> {
                assertThat(auth.id()).isEqualTo(EMPLEADO);
                assertThat(auth.companyId()).isEqualTo(LA_EMPRESA);
                assertThat(auth.authVersion()).isEqualTo(7L);
            });
        }

        @Test
        @DisplayName("un empleado dado de baja deja de tener estado autenticable")
        void un_empleado_dado_de_baja_deja_de_tener_estado_autenticable() {
            desactivarEmpleado(EMPLEADO);

            assertThat(repository.findActiveById(EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("con la EMPRESA suspendida el empleado habilitado tampoco autentica")
        void con_la_empresa_suspendida_el_empleado_habilitado_tampoco_autentica() {
            // La puerta que nadie mira. La fila del empleado esta intacta y habilitada;
            // lo unico que la deja fuera es el `c.enabled = true` sobre el JOIN FETCH.
            // Si esa condicion se cae, suspender una clinica morosa deja a todo su
            // personal dentro y no hay nada mas en el camino que lo detecte.
            desactivarEmpresa(LA_EMPRESA);

            assertThat(columna("enabled", EMPLEADO)).isEqualTo(1L);
            assertThat(repository.findActiveById(EMPLEADO)).isEmpty();
        }

        @Test
        @DisplayName("un id que no existe no se confunde con uno desactivado: los dos son vacío")
        void un_id_que_no_existe_devuelve_vacio() {
            assertThat(repository.findActiveById(-1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Rotación de la versión bajo bloqueo pesimista")
    class RotacionDeVersion {

        @Test
        @DisplayName("sube la authVersion exactamente 1 y devuelve el mismo número que queda escrito")
        void sube_la_auth_version_exactamente_uno_y_devuelve_lo_que_queda_escrito() {
            fijarVersiones(EMPLEADO, 7L, 3L);

            Optional<AuthEmployee> rotado = repository.rotateAuthVersion(EMPLEADO);
            vaciarContexto();

            // Que el record y la fila digan lo mismo es toda la invariante: el JWT se
            // firma con el numero del record y el filtro lo compara contra la columna.
            // Si se separan, el token recien emitido nace invalido.
            assertThat(rotado).get().satisfies(auth -> {
                assertThat(auth.authVersion()).isEqualTo(8L);
                assertThat(auth.companyId()).isEqualTo(LA_EMPRESA);
            });
            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(8L);
        }

        @Test
        @DisplayName("la rotación pasa por el ciclo de la entidad, así que mueve también la versión optimista")
        void la_rotacion_mueve_tambien_la_version_optimista() {
            // saveAndFlush sobre una entidad gestionada: Hibernate incrementa @Version.
            // Queda fijado porque es lo que distingue esta rotacion del UPDATE nativo de
            // bumpAuthVersion, que tiene que moverla a mano.
            fijarVersiones(EMPLEADO, 7L, 3L);

            repository.rotateAuthVersion(EMPLEADO);
            vaciarContexto();

            assertThat(columna("version", EMPLEADO)).isEqualTo(4L);
        }

        @Test
        @DisplayName("sobre un empleado dado de baja devuelve vacío y NO escribe nada")
        void sobre_un_empleado_dado_de_baja_no_escribe_nada() {
            fijarVersiones(EMPLEADO, 7L, 3L);
            desactivarEmpleado(EMPLEADO);

            assertThat(repository.rotateAuthVersion(EMPLEADO)).isEmpty();
            vaciarContexto();

            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(7L);
        }

        @Test
        @DisplayName("con la empresa suspendida devuelve vacío y NO escribe nada")
        void con_la_empresa_suspendida_no_escribe_nada() {
            fijarVersiones(EMPLEADO, 7L, 3L);
            desactivarEmpresa(LA_EMPRESA);

            assertThat(repository.rotateAuthVersion(EMPLEADO)).isEmpty();
            vaciarContexto();

            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("Invalidación de sesiones por UPDATE nativo")
    class InvalidacionDeSesiones {

        @Test
        @DisplayName("la vía ancha mueve authVersion Y version: sin la segunda, un save concurrente revive la sesión")
        void la_via_ancha_mueve_las_dos_columnas() {
            fijarVersiones(EMPLEADO, 7L, 3L);

            repository.bumpAuthVersion(EMPLEADO);
            vaciarContexto();

            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(8L);
            assertThat(columna("version", EMPLEADO)).isEqualTo(4L);
        }

        @Test
        @DisplayName("la sobrecarga acotada con la empresa propia mueve las dos columnas igual")
        void la_sobrecarga_acotada_con_la_empresa_propia_mueve_las_dos_columnas() {
            fijarVersiones(EMPLEADO, 7L, 3L);

            repository.bumpAuthVersion(EMPLEADO, LA_EMPRESA);
            vaciarContexto();

            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(8L);
            assertThat(columna("version", EMPLEADO)).isEqualTo(4L);
        }

        @Test
        @DisplayName("la sobrecarga acotada con una empresa AJENA no toca la fila")
        void la_sobrecarga_acotada_con_una_empresa_ajena_no_toca_la_fila() {
            // El `AND company_id` es la unica barrera del logout: su puerto es
            // @PreAuthorize("isAuthenticated()"), que no dice nada sobre de quien es la
            // fila. Sin el, cualquier autenticado tumba las sesiones vivas del empleado
            // de otra clinica con solo conocer un id.
            fijarVersiones(EMPLEADO, 7L, 3L);

            repository.bumpAuthVersion(EMPLEADO, LA_EMPRESA_AJENA);
            vaciarContexto();

            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(7L);
            assertThat(columna("version", EMPLEADO)).isEqualTo(3L);
        }

        @Test
        @DisplayName("la invalidación no se contagia a otro empleado de la misma empresa")
        void la_invalidacion_no_se_contagia_a_otro_empleado() {
            fijarVersiones(EMPLEADO, 7L, 3L);
            fijarVersiones(OTRO_EMPLEADO, 2L, 1L);

            repository.bumpAuthVersion(EMPLEADO, LA_EMPRESA);
            vaciarContexto();

            assertThat(columna("auth_version", OTRO_EMPLEADO)).isEqualTo(2L);
            assertThat(columna("version", OTRO_EMPLEADO)).isEqualTo(1L);
        }

        @Test
        @DisplayName("sobre un empleado ya dado de baja sigue invalidando: el UPDATE es nativo y salta el filtro")
        void sobre_un_empleado_dado_de_baja_sigue_invalidando() {
            // No es un descuido: el @SQLRestriction("enabled = true") esconde la fila de
            // JPA, pero el UPDATE va por SQL nativo. Tiene que seguir funcionando —el
            // despedido es justo a quien mas urge echar de sus sesiones vivas—.
            fijarVersiones(EMPLEADO, 7L, 3L);
            desactivarEmpleado(EMPLEADO);
            long trasLaBaja = columna("auth_version", EMPLEADO);

            repository.bumpAuthVersion(EMPLEADO);
            vaciarContexto();

            assertThat(columna("auth_version", EMPLEADO)).isEqualTo(trasLaBaja + 1L);
        }
    }
}
