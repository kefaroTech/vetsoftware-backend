package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.employee.domain.CompanyRef;
import com.vetsoftware.app.employee.domain.Employee;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Rodaja de persistencia de empleados contra MySQL real.
 *
 * <p>
 * Lo que no ve un mapper ni un service con dobles: que el
 * {@code @SQLRestriction("enabled = true")} de verdad oculta los desactivados
 * de los métodos JPA-derivados mientras que las consultas nativas (las que
 * "incluyen desactivados") lo saltan de verdad, que el soft-delete nativo no
 * dispara un {@code TransientObjectException} con empleados-hijo gestionados en
 * la sesión, y que la búsqueda paginada filtra por empresa y por texto contra
 * la base real.
 */
@Import({JpaEmployeeRepository.class, EmployeeJpaMapper.class})
@DisplayName("JpaEmployeeRepository — empleados contra MySQL real")
class EmployeePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    @Autowired
    private JpaEmployeeRepository repository;

    @Autowired
    private EmployeeJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private Employee nuevoEmpleado(String code, Long companyId, String companyName,
            String companyNit) {
        return Employee.create(code, "$2a$10$hash", "Empleado " + code,
                code.toLowerCase() + "@test.local",
                new CompanyRef(companyId, companyName, companyNit), true, false);
    }

    private Employee nuevoEmpleado(String code) {
        return nuevoEmpleado(code, COMPANY, "Veterinaria de prueba", "900123456");
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Lee una columna numerica saltandose el mapper y el contexto de persistencia.
     * Es la unica forma de afirmar que {@code version} y {@code auth_version} —dos
     * {@code BIGINT} indistinguibles en Java— quedaron cada una en su sitio: por el
     * dominio ambas son un {@code Long} y confundirlas no da error de compilacion.
     * El {@code CAST(... AS SIGNED)} normaliza tambien el {@code TINYINT} de
     * {@code enabled}, que el driver no devuelve como el mismo tipo.
     */
    private long columna(String nombre, Long id) {
        return ((Number) entityManager
                .createNativeQuery(
                        "SELECT CAST(" + nombre + " AS SIGNED) FROM employees WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    private long filasConCodigo(String code) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM employees WHERE employee_code = :code")
                .setParameter("code", code).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id, hidrata la empresa, y releer conserva los campos")
        void guardar_asigna_id_e_hidrata_la_empresa() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-100", COMPANY, "Veterinaria de prueba", "900123456"));

            assertThat(guardado.getId()).isNotNull();
            assertThat(guardado.getCompany().identifier()).isEqualTo("900123456");

            Employee leido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(leido.getEmployeeCode()).isEqualTo("EMP-100");
            assertThat(leido.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("soft-delete y reactivacion")
    class SoftDeleteYReactivacion {

        @Test
        @DisplayName("delete desactiva la fila y le sube la version de sesion, sin fallar por hijos gestionados")
        void delete_desactiva_y_sube_la_version_de_sesion() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-101", COMPANY, "Veterinaria de prueba", "900123456"));

            repository.delete(guardado.getId(), COMPANY);

            Optional<Employee> visibleParaJpaDerivado = repository.findById(guardado.getId());
            assertThat(visibleParaJpaDerivado).as("el @SQLRestriction oculta los desactivados")
                    .isEmpty();

            Employee incluyendoDesactivado = repository.findByIdIncludingDisabled(guardado.getId())
                    .orElseThrow();
            assertThat(incluyendoDesactivado.isEnabled()).isFalse();
            assertThat(incluyendoDesactivado.getAuthVersion()).isEqualTo(1L);
        }

        @Test
        @DisplayName("delete con la empresa ajena no desactiva la fila ni le toca la version")
        void delete_con_otra_empresa_no_escribe_nada() {
            // El UPDATE lleva AND company_id: el empleado de otra empresa no se desactiva
            // ni pierde sus sesiones vivas. Antes, un empleado con employee.delete dejaba
            // sin acceso al personal de cualquier tenant escribiendo su id en la URL.
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-190", COMPANY, "Veterinaria de prueba", "900123456"));

            repository.delete(guardado.getId(), OTRA_COMPANY);

            Employee intacto = repository.findByIdIncludingDisabled(guardado.getId()).orElseThrow();
            assertThat(intacto.isEnabled()).isTrue();
            assertThat(intacto.getAuthVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("findByIdIncludingDisabledAndCompanyId no ve al empleado de otra empresa")
        void el_finder_acotado_no_ve_al_empleado_ajeno() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-191", COMPANY, "Veterinaria de prueba", "900123456"));
            repository.delete(guardado.getId(), COMPANY);

            assertThat(repository.findByIdIncludingDisabledAndCompanyId(guardado.getId(), COMPANY))
                    .as("desactivado pero de mi empresa: se ve").isPresent();
            assertThat(repository.findByIdIncludingDisabledAndCompanyId(guardado.getId(),
                    OTRA_COMPANY)).as("la fila ajena es un 404, no una baja").isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar y sube otra vez la version de sesion")
        void reactivate_vuelve_a_habilitar() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-102", COMPANY, "Veterinaria de prueba", "900123456"));
            repository.delete(guardado.getId(), COMPANY);

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            Employee reactivado = repository.findById(guardado.getId()).orElseThrow();
            assertThat(reactivado.isEnabled()).isTrue();
            assertThat(reactivado.getAuthVersion()).isEqualTo(2L);
        }

        @Test
        @DisplayName("reactivate sobre un id inexistente no afecta ninguna fila")
        void reactivate_sobre_id_inexistente_no_afecta_filas() {
            int filas = repository.reactivate(999999L);

            assertThat(filas).isZero();
        }

        @Test
        @DisplayName("reactivate acotado a su empresa vuelve a habilitar y sube la version")
        void reactivate_acotado_a_su_empresa_reactiva() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-110", COMPANY, "Veterinaria de prueba", "900123456"));
            repository.delete(guardado.getId(), COMPANY);

            int filas = repository.reactivate(guardado.getId(), COMPANY);

            assertThat(filas).isEqualTo(1);
            Employee reactivado = repository.findById(guardado.getId()).orElseThrow();
            assertThat(reactivado.isEnabled()).isTrue();
            assertThat(reactivado.getAuthVersion()).isEqualTo(2L);
        }

        /**
         * La fuga que cierra este test: sin el {@code AND company_id}, este UPDATE
         * afectaba la fila y le devolvia el login —con su {@code auth_version} al dia—
         * a un empleado que otra empresa habia despedido. En la reactivacion no hay
         * lectura previa que valide la propiedad: el SQL es la unica barrera.
         */
        @Test
        @DisplayName("reactivate con el companyId de otra empresa afecta 0 filas y lo deja desactivado")
        void reactivate_desde_otra_empresa_no_afecta_ninguna_fila() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-111", COMPANY, "Veterinaria de prueba", "900123456"));
            repository.delete(guardado.getId(), COMPANY);

            int filas = repository.reactivate(guardado.getId(), OTRA_COMPANY);

            assertThat(filas).isZero();
            // El orden importa: findByIdIncludingDisabled es nativa y deja la entidad
            // en el contexto de persistencia, asi que un findById posterior la
            // serviria desde la cache de primer nivel sin aplicar el @SQLRestriction.
            assertThat(repository.findById(guardado.getId())).as("sigue fuera del listado activo")
                    .isEmpty();
            Employee seguido = repository.findByIdIncludingDisabled(guardado.getId()).orElseThrow();
            assertThat(seguido.isEnabled()).isFalse();
            assertThat(seguido.getAuthVersion()).as("no se le renovo la sesion").isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("listado por empresa")
    class ListadoPorEmpresa {

        @Test
        @DisplayName("findAllByCompanyIdIncludingDisabled trae activos y desactivados de esa empresa")
        void trae_activos_y_desactivados_de_la_empresa() {
            Employee activo = repository
                    .save(nuevoEmpleado("EMP-103", COMPANY, "Veterinaria de prueba", "900123456"));
            Employee desactivado = repository
                    .save(nuevoEmpleado("EMP-104", COMPANY, "Veterinaria de prueba", "900123456"));
            repository.delete(desactivado.getId(), COMPANY);

            List<Employee> empleados = repository.findAllByCompanyIdIncludingDisabled(COMPANY);

            assertThat(empleados).extracting(Employee::getId).contains(activo.getId(),
                    desactivado.getId());
        }

        @Test
        @DisplayName("una empresa ajena no ve los empleados de otra empresa")
        void una_empresa_ajena_no_ve_los_empleados_de_otra() {
            repository
                    .save(nuevoEmpleado("EMP-105", COMPANY, "Veterinaria de prueba", "900123456"));

            List<Employee> empleadosDeLaOtra = repository
                    .findAllByCompanyIdIncludingDisabled(OTRA_COMPANY);

            assertThat(empleadosDeLaOtra).extracting(Employee::getEmployeeCode)
                    .doesNotContain("EMP-105");
        }
    }

    @Nested
    @DisplayName("busqueda paginada")
    class BusquedaPaginada {

        @Test
        @DisplayName("search filtra por empresa y por texto (nombre/codigo/correo)")
        void search_filtra_por_empresa_y_por_texto() {
            repository
                    .save(nuevoEmpleado("EMP-106", COMPANY, "Veterinaria de prueba", "900123456"));
            repository
                    .save(nuevoEmpleado("EMP-200", COMPANY, "Veterinaria de prueba", "900123456"));

            PageResult<Employee> pagina = repository
                    .search(new SearchEmployeesCommand(COMPANY, "EMP-106", 0, 15));

            assertThat(pagina.content()).extracting(Employee::getEmployeeCode)
                    .containsExactly("EMP-106");
            assertThat(pagina.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("una busqueda sin texto trae todos los de la empresa, sin filas de otra")
        void una_busqueda_sin_texto_trae_todos_los_de_la_empresa() {
            repository
                    .save(nuevoEmpleado("EMP-107", COMPANY, "Veterinaria de prueba", "900123456"));
            repository
                    .save(nuevoEmpleado("EMP-108", OTRA_COMPANY, "Veterinaria ajena", "900654321"));

            PageResult<Employee> pagina = repository
                    .search(new SearchEmployeesCommand(COMPANY, null, 0, 15));

            assertThat(pagina.content()).extracting(Employee::getEmployeeCode).contains("EMP-107")
                    .doesNotContain("EMP-108");
        }
    }

    @Nested
    @DisplayName("unicidad del codigo")
    class UnicidadDelCodigo {

        @Test
        @DisplayName("codeExists cuenta incluso los codigos de empleados desactivados")
        void code_exists_cuenta_tambien_desactivados() {
            Employee guardado = repository
                    .save(nuevoEmpleado("EMP-109", COMPANY, "Veterinaria de prueba", "900123456"));
            repository.delete(guardado.getId(), COMPANY);

            assertThat(repository.codeExists("EMP-109")).isTrue();
        }

        @Test
        @DisplayName("codeExists es falso para un codigo nunca usado")
        void code_exists_es_falso_para_codigo_nunca_usado() {
            assertThat(repository.codeExists("EMP-NUNCA-USADO")).isFalse();
        }
    }

    /**
     * BE-26. {@code employees} es, junto con {@code system_users}, la unica tabla
     * con dos columnas de version del mismo tipo: la {@code version} de bloqueo
     * optimista que gestiona Hibernate y la {@code auth_version} preexistente que
     * invalida sesiones (viaja en el claim {@code authVersion} del JWT). Ambas son
     * {@code Long}, asi que intercambiarlas no da error de compilacion y ningun
     * test con dobles lo veria: estas aserciones leen las dos columnas crudas.
     */
    @Nested
    @DisplayName("bloqueo optimista y auth_version")
    class BloqueoOptimista {

        @Test
        @DisplayName("dos copias de la misma fila: la segunda en guardar choca por version obsoleta")
        void la_segunda_copia_choca_por_version_obsoleta() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-300"));
            releerDesdeLaBase();

            Employee primeraCopia = repository.findById(guardado.getId()).orElseThrow();
            Employee segundaCopia = repository.findById(guardado.getId()).orElseThrow();

            primeraCopia.update("EMP-301", "Nombre Uno", "uno@test.local");
            repository.save(primeraCopia);
            releerDesdeLaBase();

            segundaCopia.update("EMP-302", "Nombre Dos", "dos@test.local");

            assertThatThrownBy(() -> repository.save(segundaCopia))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("EmployeeJpaEntity");
        }

        @Test
        @DisplayName("una edicion normal sube version y deja auth_version intacta")
        void una_edicion_normal_sube_version_y_deja_auth_version_intacta() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-310"));
            releerDesdeLaBase();

            Employee leido = repository.findById(guardado.getId()).orElseThrow();
            leido.update("EMP-311", "Nombre Editado", "editado@test.local");
            repository.save(leido);
            releerDesdeLaBase();

            assertThat(columna("version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("auth_version", guardado.getId()))
                    .as("renombrar a alguien no le tumba las sesiones vivas").isZero();
        }

        @Test
        @DisplayName("resetPassword sube auth_version y tambien version, cada una una sola vez")
        void reset_password_sube_auth_version_y_tambien_version() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-320"));
            releerDesdeLaBase();

            // Mismo camino que ResetEmployeePasswordService: leer, resetPassword, guardar.
            Employee leido = repository.findByIdIncludingDisabled(guardado.getId()).orElseThrow();
            leido.resetPassword("$2a$10$hash-nuevo");
            repository.save(leido);
            releerDesdeLaBase();

            assertThat(columna("auth_version", guardado.getId()))
                    .as("el reset revoca las sesiones vivas").isEqualTo(1L);
            assertThat(columna("version", guardado.getId()))
                    .as("y sigue siendo un UPDATE, con su version de bloqueo al dia").isEqualTo(1L);
        }

        @Test
        @DisplayName("resetPassword no desactiva el bloqueo optimista para una copia obsoleta")
        void reset_password_no_desactiva_el_bloqueo_optimista() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-330"));
            releerDesdeLaBase();

            Employee copiaObsoleta = repository.findById(guardado.getId()).orElseThrow();
            Employee elQueResetea = repository.findById(guardado.getId()).orElseThrow();

            elQueResetea.resetPassword("$2a$10$hash-nuevo");
            repository.save(elQueResetea);
            releerDesdeLaBase();

            copiaObsoleta.update("EMP-331", "Nombre Tardio", "tardio@test.local");

            assertThatThrownBy(() -> repository.save(copiaObsoleta))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("EmployeeJpaEntity");
        }

        @Test
        @DisplayName("el @SQLDelete de la entidad sube auth_version pero NO version, al reves que deactivate")
        void el_sql_delete_de_la_entidad_sube_auth_version_pero_no_version() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-340"));
            releerDesdeLaBase();

            // OJO, hay DOS bajas logicas distintas sobre esta tabla y dejan la fila en
            // estados distintos. Las dos ponen `enabled = false` y suben auth_version:
            //
            // 1. Este @SQLDelete de la entidad, que dispara `deleteById`. NO sube
            // `version`: un DELETE solo la LEE, en su `WHERE id = ? AND version = ?`.
            // 2. El UPDATE nativo `deactivate` del puerto de dominio, que SI la sube
            // (ver `la_baja_por_deactivate_sube_auth_version_y_tambien_version`).
            //
            // Ningun caso de uso llega al camino 1 —el puerto siempre va por `deactivate`—
            // asi que se ataca aqui el repositorio Spring Data a proposito, o el
            // @SQLDelete quedaria sin red. Sus dos parametros son de tipos
            // indistinguibles: si Hibernate ligara el id donde va la version, el UPDATE
            // afectaria 0 filas y explotaria por StaleState.
            jpaRepository.deleteById(guardado.getId());
            releerDesdeLaBase();

            assertThat(columna("enabled", guardado.getId())).isZero();
            assertThat(columna("auth_version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("version", guardado.getId()))
                    .as("un DELETE no incrementa la version de bloqueo, solo la lee").isZero();
        }

        @Test
        @DisplayName("la baja por deactivate sube auth_version y tambien version")
        void la_baja_por_deactivate_sube_auth_version_y_tambien_version() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-390"));
            releerDesdeLaBase();

            // Esta es la baja que ejercita produccion: el puerto de dominio no usa el
            // @SQLDelete de arriba sino este UPDATE nativo, que si mueve `version`.
            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(columna("enabled", guardado.getId())).isZero();
            assertThat(columna("auth_version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("version", guardado.getId()))
                    .as("la baja mueve tambien la version de bloqueo").isEqualTo(1L);
        }

        @Test
        @DisplayName("la reactivacion sube auth_version y tambien version, encadenada sobre la baja")
        void la_reactivacion_sube_auth_version_y_tambien_version() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-400"));
            releerDesdeLaBase();
            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            repository.reactivate(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(columna("enabled", guardado.getId())).isEqualTo(1L);
            assertThat(columna("auth_version", guardado.getId())).isEqualTo(2L);
            assertThat(columna("version", guardado.getId()))
                    .as("baja y reactivacion son dos UPDATE: la version avanza en los dos")
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("guardar un empleado existente actualiza la fila y no inserta otra")
        void guardar_un_empleado_existente_actualiza_y_no_inserta_otra() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-350"));
            releerDesdeLaBase();

            Employee leido = repository.findById(guardado.getId()).orElseThrow();
            leido.update("EMP-350", "Nombre Editado", "editado350@test.local");
            repository.save(leido);
            releerDesdeLaBase();

            assertThat(filasConCodigo("EMP-350")).isEqualTo(1L);
            assertThat(repository.findAllByCompanyIdIncludingDisabled(COMPANY))
                    .filteredOn(e -> "EMP-350".equals(e.getEmployeeCode())).hasSize(1);
            assertThat(columna("version", guardado.getId())).isEqualTo(1L);
        }

        @Test
        @DisplayName("el bump de auth_version sube tambien la version de bloqueo")
        void el_bump_de_auth_version_sube_tambien_la_version_de_bloqueo() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-360"));
            releerDesdeLaBase();

            jpaRepository.bumpAuthVersion(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(columna("auth_version", guardado.getId())).isEqualTo(1L);
            assertThat(columna("version", guardado.getId()))
                    .as("el UPDATE de revocacion mueve las dos columnas, no solo auth_version")
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("una copia leida antes del bump de auth_version ya no puede pisar la revocacion")
        void una_copia_previa_al_bump_ya_no_puede_pisar_la_revocacion() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-370"));
            releerDesdeLaBase();
            Employee copiaPrevia = repository.findById(guardado.getId()).orElseThrow();

            jpaRepository.bumpAuthVersion(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            copiaPrevia.update("EMP-371", "Nombre Tardio", "tardio370@test.local");

            // Incidencia #54. Esto es lo que protege el `version = version + 1` del
            // UPDATE de revocacion, y por eso ese incremento no es decorativo: el
            // UPDATE es nativo y no pasa por Hibernate, asi que sin el la columna
            // `version` no se movia y el candado optimista no veia ningun conflicto.
            // Una edicion del administrador cargada ANTES del logout casaba su
            // `WHERE version = ?` y reescribia `auth_version` con el valor viejo que
            // llevaba en el dominio —el mapper la copia campo a campo—, revalidando
            // en silencio un token ya revocado. Movida la version, esa edicion pierde
            // la carrera aqui y el llamador recibe un 409 CONCURRENT_MODIFICATION.
            assertThatThrownBy(() -> repository.save(copiaPrevia))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("EmployeeJpaEntity");

            entityManager.clear();
            assertThat(columna("auth_version", guardado.getId()))
                    .as("la revocacion sobrevive: la edicion perdedora no llego a escribir")
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("una ficha cargada antes de la baja ya no puede resucitar al empleado dado de baja")
        void una_ficha_cargada_antes_de_la_baja_ya_no_resucita_al_empleado() {
            Employee guardado = repository.save(nuevoEmpleado("EMP-380"));
            releerDesdeLaBase();
            // El administrador abre la ficha: se la lleva con enabled = true,
            // auth_version = 0 y version = 0.
            Employee fichaDelAdmin = repository.findById(guardado.getId()).orElseThrow();

            // Mientras tanto, a ese empleado lo despiden.
            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            fichaDelAdmin.update("EMP-381", "Nombre Tardio", "tardio380@test.local");

            // Incidencia #54, en su version peor. El administrador guarda una ficha que
            // se cargo ANTES de la baja, y el mapper escribe la entidad ENTERA desde ese
            // dominio: no solo devolveria `auth_version` a 0 —revalidando las sesiones
            // del despedido— sino tambien `enabled` a true, readmitiendolo. Y no habria
            // saltado nada, porque la baja se hace con un UPDATE nativo que no pasa por
            // Hibernate: sin el `version = version + 1` de `deactivate`, el
            // `WHERE version = ?` de este save casaba y el despido se deshacia solo.
            // Movida la version, el save pierde la carrera y el llamador recibe un 409
            // CONCURRENT_MODIFICATION en vez de un empleado readmitido en silencio.
            assertThatThrownBy(() -> repository.save(fichaDelAdmin))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class)
                    .hasMessageContaining("EmployeeJpaEntity");

            entityManager.clear();
            assertThat(columna("enabled", guardado.getId())).as("sigue dado de baja").isZero();
            assertThat(columna("auth_version", guardado.getId()))
                    .as("y sus sesiones siguen tumbadas").isEqualTo(1L);
        }
    }
}
