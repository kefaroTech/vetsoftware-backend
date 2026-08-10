package com.vetsoftware.app.employee.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VOs del empleado: copian datos de otras features (empresa, sede,
 * rol) para poder mostrarlos sin importar su dominio. Como son copias, un campo
 * en blanco no lo detecta ninguna FK: la unica red es el constructor compacto
 * del record, y eso es lo que se prueba aqui.
 */
@DisplayName("Companion VOs del empleado")
class EmployeeRefsTest {

    static Stream<Arguments> refsIncompletas() {
        return Stream.of(
                arguments("sede sin id", (ThrowingCallable) () -> new BranchRef(null, "Sede Norte"),
                        "branch id is required"),
                arguments("sede sin nombre", (ThrowingCallable) () -> new BranchRef(7L, null),
                        "branch name is required"),
                arguments("sede con nombre en blanco",
                        (ThrowingCallable) () -> new BranchRef(7L, "   "),
                        "branch name is required"),
                arguments("empresa sin id",
                        (ThrowingCallable) () -> new CompanyRef(null, "Vetrina", "900123456"),
                        "company id is required"),
                arguments("empresa sin nombre",
                        (ThrowingCallable) () -> new CompanyRef(9L, null, "900123456"),
                        "company name is required"),
                arguments("empresa con nombre en blanco",
                        (ThrowingCallable) () -> new CompanyRef(9L, " ", "900123456"),
                        "company name is required"),
                arguments("empresa sin identificacion tributaria",
                        (ThrowingCallable) () -> new CompanyRef(9L, "Vetrina", null),
                        "company identifier is required"),
                arguments("empresa con identificacion en blanco",
                        (ThrowingCallable) () -> new CompanyRef(9L, "Vetrina", ""),
                        "company identifier is required"),
                arguments("rol sin id de asignacion",
                        (ThrowingCallable) () -> new RoleSnapshot(null, 3L, "Veterinario", "VET"),
                        "employeeRoleId is required"),
                arguments("rol sin id",
                        (ThrowingCallable) () -> new RoleSnapshot(500L, null, "Veterinario", "VET"),
                        "role id is required"),
                arguments("rol sin nombre",
                        (ThrowingCallable) () -> new RoleSnapshot(500L, 3L, null, "VET"),
                        "role name is required"),
                arguments("rol con nombre en blanco",
                        (ThrowingCallable) () -> new RoleSnapshot(500L, 3L, "  ", "VET"),
                        "role name is required"),
                arguments("rol sin codigo",
                        (ThrowingCallable) () -> new RoleSnapshot(500L, 3L, "Veterinario", null),
                        "role code is required"),
                arguments("rol con codigo en blanco",
                        (ThrowingCallable) () -> new RoleSnapshot(500L, 3L, "Veterinario", " "),
                        "role code is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refsIncompletas")
    @DisplayName("una referencia incompleta no llega a construirse")
    void una_referencia_incompleta_no_llega_a_construirse(String caso, ThrowingCallable creacion,
            String mensaje) {
        assertThatThrownBy(creacion).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(mensaje);
    }

    @Nested
    @DisplayName("BranchRef")
    class Sede {

        @Test
        @DisplayName("conserva id y nombre tal cual llegan")
        void conserva_id_y_nombre() {
            BranchRef ref = new BranchRef(7L, "Sede Norte");

            assertThat(ref.id()).isEqualTo(7L);
            assertThat(ref.name()).isEqualTo("Sede Norte");
        }

        @Test
        @DisplayName("dos referencias a la misma sede son iguales por valor")
        void dos_referencias_a_la_misma_sede_son_iguales() {
            assertThat(new BranchRef(7L, "Sede Norte")).isEqualTo(new BranchRef(7L, "Sede Norte"))
                    .hasSameHashCodeAs(new BranchRef(7L, "Sede Norte"));
        }

        @Test
        @DisplayName("un nombre distinto es otra referencia: el VO copia datos, no apunta")
        void un_nombre_distinto_es_otra_referencia() {
            assertThat(new BranchRef(7L, "Sede Norte")).isNotEqualTo(new BranchRef(7L, "Sede Sur"));
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class Empresa {

        @Test
        @DisplayName("conserva id, razon social e identificacion tributaria")
        void conserva_los_tres_campos() {
            CompanyRef ref = new CompanyRef(9L, "Veterinaria Vetrina", "900123456");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Veterinaria Vetrina");
            assertThat(ref.identifier()).isEqualTo("900123456");
        }

        @Test
        @DisplayName("no cruza el nombre con la identificacion — ambos son String")
        void no_cruza_el_nombre_con_la_identificacion() {
            CompanyRef ref = new CompanyRef(9L, "900123456", "Veterinaria Vetrina");

            assertThat(ref.name()).isEqualTo("900123456");
            assertThat(ref.identifier()).isEqualTo("Veterinaria Vetrina");
        }

        @Test
        @DisplayName("dos referencias a la misma empresa son iguales por valor")
        void dos_referencias_a_la_misma_empresa_son_iguales() {
            assertThat(new CompanyRef(9L, "Vetrina", "900123456"))
                    .isEqualTo(new CompanyRef(9L, "Vetrina", "900123456"));
        }
    }

    @Nested
    @DisplayName("RoleSnapshot")
    class Rol {

        @Test
        @DisplayName("distingue el id de la asignacion del id del rol")
        void distingue_el_id_de_la_asignacion_del_id_del_rol() {
            // employeeRoleId identifica la FILA employee_roles (lo que se revoca);
            // id identifica el ROL. Cruzarlos revoca el rol equivocado.
            RoleSnapshot rol = new RoleSnapshot(500L, 3L, "Veterinario", "VET");

            assertThat(rol.employeeRoleId()).isEqualTo(500L);
            assertThat(rol.id()).isEqualTo(3L);
            assertThat(rol.name()).isEqualTo("Veterinario");
            assertThat(rol.code()).isEqualTo("VET");
        }

        @Test
        @DisplayName("el mismo rol asignado dos veces son snapshots distintos")
        void el_mismo_rol_asignado_dos_veces_son_snapshots_distintos() {
            assertThat(new RoleSnapshot(500L, 3L, "Veterinario", "VET"))
                    .isNotEqualTo(new RoleSnapshot(501L, 3L, "Veterinario", "VET"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "VET", "CASHIER"})
        @DisplayName("acepta cualquier codigo de rol no vacio")
        void acepta_cualquier_codigo_de_rol_no_vacio(String code) {
            assertThatCode(() -> new RoleSnapshot(500L, 3L, "Un rol", code))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("EmployeeId")
    class IdentificadorDeEmpleado {

        @Test
        @DisplayName("of es equivalente al constructor")
        void of_es_equivalente_al_constructor() {
            assertThat(EmployeeId.of(55L)).isEqualTo(new EmployeeId(55L));
        }

        @Test
        @DisplayName("dos ids con el mismo valor son iguales")
        void dos_ids_con_el_mismo_valor_son_iguales() {
            assertThat(EmployeeId.of(55L)).isEqualTo(EmployeeId.of(55L))
                    .hasSameHashCodeAs(EmployeeId.of(55L));
        }

        @Test
        @DisplayName("ids distintos no se confunden")
        void ids_distintos_no_se_confunden() {
            assertThat(EmployeeId.of(55L)).isNotEqualTo(EmployeeId.of(56L));
        }

        @Test
        @DisplayName("admite el valor nulo del empleado aun no persistido")
        void admite_el_valor_nulo_del_empleado_aun_no_persistido() {
            // Comportamiento documentado: el VO no valida, porque el id lo asigna la BD
            // y hasta el INSERT no existe.
            assertThat(EmployeeId.of(null).value()).isNull();
        }
    }
}
