package com.vetsoftware.app.employee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
}
