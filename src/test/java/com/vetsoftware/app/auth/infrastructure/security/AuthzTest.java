package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.testsupport.AuthMother;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@code Authz} decide autorización a partir del {@code AuthContext} que deja
 * el {@code AuthFilter} en el {@code SecurityContextHolder}. No tiene puertos
 * que mockear: es JUnit puro manipulando el contexto de seguridad, igual que
 * {@code SystemRoleIsolationTest}.
 */
class AuthzTest {

    private final Authz authz = new Authz();

    @AfterEach
    void limpiarContextos() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static void autenticar(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static void conCabeceraDeEmpresa(String valor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (valor != null) {
            request.addHeader(Authz.COMPANY_SCOPE_HEADER, valor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Nested
    @DisplayName("isMyCompany")
    class EsMiEmpresa {

        @Test
        @DisplayName("companyId nulo nunca es mi empresa, sin necesidad de contexto")
        void companyId_nulo_es_falso() {
            assertThat(authz.isMyCompany(null)).isFalse();
        }

        @Test
        @DisplayName("un empleado con la misma empresa es su propia empresa")
        void empleado_con_la_misma_empresa() {
            autenticar(AuthMother.empleado());
            assertThat(authz.isMyCompany(AuthMother.COMPANY_ID)).isTrue();
        }

        @Test
        @DisplayName("un empleado de otra empresa no coincide")
        void empleado_de_otra_empresa() {
            autenticar(AuthMother.empleado());
            assertThat(authz.isMyCompany(999L)).isFalse();
        }

        @Test
        @DisplayName("un usuario de sistema nunca es 'mi empresa'")
        void usuario_de_sistema_nunca_coincide() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.isMyCompany(AuthMother.COMPANY_ID)).isFalse();
        }

        @Test
        @DisplayName("el proceso interno de sistema nunca es 'mi empresa'")
        void system_context_nunca_coincide() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.isMyCompany(AuthMother.COMPANY_ID)).isFalse();
        }

        @Test
        @DisplayName("sin autenticación no hay empresa propia")
        void sin_autenticacion_es_falso() {
            assertThat(authz.isMyCompany(AuthMother.COMPANY_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("currentCompanyId")
    class EmpresaActual {

        @Test
        @DisplayName("un empleado devuelve la empresa de su contexto")
        void empleado_devuelve_su_empresa() {
            autenticar(AuthMother.empleado());
            assertThat(authz.currentCompanyId()).isEqualTo(AuthMother.COMPANY_ID);
        }

        @Test
        @DisplayName("un usuario de sistema exige la cabecera X-Company-Id")
        void usuario_de_sistema_usa_la_cabecera() {
            autenticar(AuthMother.usuarioDeSistema());
            conCabeceraDeEmpresa("9");

            assertThat(authz.currentCompanyId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("sin request activa, un usuario de sistema no puede resolver empresa")
        void usuario_de_sistema_sin_request_falla() {
            autenticar(AuthMother.usuarioDeSistema());

            assertThatThrownBy(authz::currentCompanyId).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(Authz.COMPANY_SCOPE_HEADER);
        }

        @Test
        @DisplayName("sin la cabecera, un usuario de sistema falla explícitamente")
        void usuario_de_sistema_sin_cabecera_falla() {
            autenticar(AuthMother.usuarioDeSistema());
            conCabeceraDeEmpresa(null);

            assertThatThrownBy(authz::currentCompanyId).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(Authz.COMPANY_SCOPE_HEADER);
        }

        @Test
        @DisplayName("una cabecera en blanco cuenta como ausente")
        void cabecera_en_blanco_falla() {
            autenticar(AuthMother.usuarioDeSistema());
            conCabeceraDeEmpresa("   ");

            assertThatThrownBy(authz::currentCompanyId)
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("una cabecera no numérica falla con mensaje explícito")
        void cabecera_no_numerica_falla() {
            autenticar(AuthMother.usuarioDeSistema());
            conCabeceraDeEmpresa("abc");

            assertThatThrownBy(authz::currentCompanyId).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive integer");
        }

        @Test
        @DisplayName("una cabecera no positiva falla igual que una no numérica")
        void cabecera_no_positiva_falla() {
            autenticar(AuthMother.usuarioDeSistema());
            conCabeceraDeEmpresa("-5");

            assertThatThrownBy(authz::currentCompanyId).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive integer");
        }

        @Test
        @DisplayName("el proceso interno de sistema no tiene empresa")
        void system_context_no_tiene_empresa() {
            autenticar(SystemContext.INSTANCE);

            assertThatThrownBy(authz::currentCompanyId).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("sin autenticación no hay empresa")
        void sin_autenticacion_no_hay_empresa() {
            assertThatThrownBy(authz::currentCompanyId).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("currentCompanyIdOrNull")
    class EmpresaActualOpcional {

        @Test
        @DisplayName("un empleado devuelve su empresa")
        void empleado_devuelve_su_empresa() {
            autenticar(AuthMother.empleado());
            assertThat(authz.currentCompanyIdOrNull()).isEqualTo(AuthMother.COMPANY_ID);
        }

        @Test
        @DisplayName("un usuario de sistema devuelve null sin mirar la cabecera")
        void usuario_de_sistema_devuelve_null() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.currentCompanyIdOrNull()).isNull();
        }

        @Test
        @DisplayName("el proceso interno devuelve null")
        void system_context_devuelve_null() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.currentCompanyIdOrNull()).isNull();
        }

        @Test
        @DisplayName("sin autenticación devuelve null")
        void sin_autenticacion_devuelve_null() {
            assertThat(authz.currentCompanyIdOrNull()).isNull();
        }
    }

    @Nested
    @DisplayName("currentEmployeeId / isCurrentEmployee / currentEmployeeIdOrNull")
    class EmpleadoActual {

        @Test
        @DisplayName("un empleado devuelve su propio id")
        void empleado_devuelve_su_id() {
            autenticar(AuthMother.empleado());
            assertThat(authz.currentEmployeeId()).isEqualTo(AuthMother.EMPLOYEE_ID);
        }

        @Test
        @DisplayName("un usuario de sistema no tiene id de empleado")
        void usuario_de_sistema_no_tiene_empleado() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThatThrownBy(authz::currentEmployeeId).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("el proceso interno no tiene id de empleado")
        void system_context_no_tiene_empleado() {
            autenticar(SystemContext.INSTANCE);
            assertThatThrownBy(authz::currentEmployeeId).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("sin autenticación no hay empleado")
        void sin_autenticacion_no_hay_empleado() {
            assertThatThrownBy(authz::currentEmployeeId).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("employeeId nulo nunca es el empleado actual")
        void employeeId_nulo_es_falso() {
            assertThat(authz.isCurrentEmployee(null)).isFalse();
        }

        @Test
        @DisplayName("el propio id del empleado autenticado coincide")
        void el_propio_id_coincide() {
            autenticar(AuthMother.empleado());
            assertThat(authz.isCurrentEmployee(AuthMother.EMPLOYEE_ID)).isTrue();
        }

        @Test
        @DisplayName("un id ajeno no coincide")
        void un_id_ajeno_no_coincide() {
            autenticar(AuthMother.empleado());
            assertThat(authz.isCurrentEmployee(999L)).isFalse();
        }

        @Test
        @DisplayName("un usuario de sistema nunca es 'el empleado actual'")
        void usuario_de_sistema_nunca_es_empleado() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.isCurrentEmployee(AuthMother.SYSTEM_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("el proceso interno nunca es 'el empleado actual'")
        void system_context_nunca_es_empleado() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.isCurrentEmployee(1L)).isFalse();
        }

        @Test
        @DisplayName("sin autenticación nunca es el empleado actual")
        void sin_autenticacion_nunca_es_empleado() {
            assertThat(authz.isCurrentEmployee(1L)).isFalse();
        }

        @Test
        @DisplayName("variante sin lanzar: un empleado devuelve su id")
        void or_null_empleado_devuelve_su_id() {
            autenticar(AuthMother.empleado());
            assertThat(authz.currentEmployeeIdOrNull()).isEqualTo(AuthMother.EMPLOYEE_ID);
        }

        @Test
        @DisplayName("variante sin lanzar: un usuario de sistema devuelve null")
        void or_null_usuario_de_sistema_devuelve_null() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.currentEmployeeIdOrNull()).isNull();
        }

        @Test
        @DisplayName("variante sin lanzar: el proceso interno devuelve null")
        void or_null_system_context_devuelve_null() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.currentEmployeeIdOrNull()).isNull();
        }

        @Test
        @DisplayName("variante sin lanzar: sin autenticación devuelve null")
        void or_null_sin_autenticacion_devuelve_null() {
            assertThat(authz.currentEmployeeIdOrNull()).isNull();
        }
    }

    @Nested
    @DisplayName("currentSystemUserId — la firma de una decisión comercial")
    class UsuarioDeSistemaActual {

        @Test
        @DisplayName("una cuenta de plataforma devuelve su id")
        void usuario_de_sistema_devuelve_su_id() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.currentSystemUserId()).isEqualTo(AuthMother.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("un empleado no puede firmar como plataforma")
        void un_empleado_no_puede_firmar_como_plataforma() {
            autenticar(AuthMother.empleado());
            assertThatThrownBy(() -> authz.currentSystemUserId())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No system user context");
        }

        @Test
        @DisplayName("el proceso interno no tiene cuenta que firmar")
        void el_proceso_interno_no_tiene_cuenta_que_firmar() {
            autenticar(SystemContext.INSTANCE);
            assertThatThrownBy(() -> authz.currentSystemUserId())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No system user context");
        }

        @Test
        @DisplayName("sin autenticación no hay firma posible")
        void sin_autenticacion_no_hay_firma_posible() {
            assertThatThrownBy(() -> authz.currentSystemUserId())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No system user context");
        }

        @Test
        @DisplayName("variante sin lanzar: una cuenta de plataforma devuelve su id")
        void or_null_usuario_de_sistema_devuelve_su_id() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.currentSystemUserIdOrNull()).isEqualTo(AuthMother.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("variante sin lanzar: un empleado devuelve null")
        void or_null_empleado_devuelve_null() {
            autenticar(AuthMother.empleado());
            assertThat(authz.currentSystemUserIdOrNull()).isNull();
        }

        @Test
        @DisplayName("variante sin lanzar: el proceso interno devuelve null")
        void or_null_system_context_devuelve_null_en_sistema() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.currentSystemUserIdOrNull()).isNull();
        }

        @Test
        @DisplayName("variante sin lanzar: sin autenticación devuelve null")
        void or_null_sin_autenticacion_devuelve_null_en_sistema() {
            assertThat(authz.currentSystemUserIdOrNull()).isNull();
        }
    }

    @Nested
    @DisplayName("isSuperAdmin")
    class SuperAdmin {

        @Test
        @DisplayName("solo un usuario de sistema es superadmin")
        void usuario_de_sistema_es_superadmin() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.isSuperAdmin()).isTrue();
        }

        @Test
        @DisplayName("un empleado nunca es superadmin, ni siquiera el rol ADMIN")
        void empleado_no_es_superadmin() {
            autenticar(AuthMother.empleado());
            assertThat(authz.isSuperAdmin()).isFalse();
        }

        @Test
        @DisplayName("el proceso interno de sistema no es 'superadmin' en este sentido")
        void system_context_no_es_superadmin() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.isSuperAdmin()).isFalse();
        }

        @Test
        @DisplayName("sin autenticación no hay superadmin")
        void sin_autenticacion_no_es_superadmin() {
            assertThat(authz.isSuperAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("alcance de sedes")
    class Sedes {

        @Test
        @DisplayName("currentBranchIds devuelve las sedes del empleado")
        void current_branch_ids_devuelve_las_sedes() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID, 20L)));
            assertThat(authz.currentBranchIds()).containsExactlyInAnyOrder(AuthMother.BRANCH_ID,
                    20L);
        }

        @Test
        @DisplayName("currentBranchIds de un usuario de sistema lanza")
        void current_branch_ids_usuario_de_sistema_lanza() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThatThrownBy(authz::currentBranchIds).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("currentBranchIds del proceso interno lanza")
        void current_branch_ids_system_context_lanza() {
            autenticar(SystemContext.INSTANCE);
            assertThatThrownBy(authz::currentBranchIds).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("currentBranchIds sin autenticación lanza")
        void current_branch_ids_sin_autenticacion_lanza() {
            assertThatThrownBy(authz::currentBranchIds).isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("currentBranchIdsOrEmpty devuelve las sedes del empleado")
        void or_empty_devuelve_las_sedes() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            assertThat(authz.currentBranchIdsOrEmpty()).containsExactly(AuthMother.BRANCH_ID);
        }

        @Test
        @DisplayName("currentBranchIdsOrEmpty nunca lanza aunque el set del empleado sea null")
        void or_empty_con_sedes_null_no_lanza() {
            autenticar(AuthMother.empleado(Set.of(), null));
            assertThat(authz.currentBranchIdsOrEmpty()).isEmpty();
        }

        @Test
        @DisplayName("currentBranchIdsOrEmpty de un usuario de sistema es vacío, no lanza")
        void or_empty_usuario_de_sistema_es_vacio() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.currentBranchIdsOrEmpty()).isEmpty();
        }

        @Test
        @DisplayName("currentBranchIdsOrEmpty del proceso interno es vacío")
        void or_empty_system_context_es_vacio() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.currentBranchIdsOrEmpty()).isEmpty();
        }

        @Test
        @DisplayName("currentBranchIdsOrEmpty sin autenticación es vacío")
        void or_empty_sin_autenticacion_es_vacio() {
            assertThat(authz.currentBranchIdsOrEmpty()).isEmpty();
        }

        @Test
        @DisplayName("requireAssignableBranches no hace nada con una colección nula")
        void require_assignable_con_null_no_hace_nada() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            authz.requireAssignableBranches(null);
        }

        @Test
        @DisplayName("requireAssignableBranches acepta sedes dentro del alcance")
        void require_assignable_acepta_sedes_propias() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID, 20L)));
            authz.requireAssignableBranches(List.of(AuthMother.BRANCH_ID));
        }

        @Test
        @DisplayName("requireAssignableBranches rechaza una sede ajena")
        void require_assignable_rechaza_sede_ajena() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            assertThatThrownBy(
                    () -> authz.requireAssignableBranches(List.of(AuthMother.OTHER_BRANCH_ID)))
                    .isInstanceOf(BranchAccessDeniedException.class)
                    .hasMessageContaining(String.valueOf(AuthMother.OTHER_BRANCH_ID));
        }

        @Test
        @DisplayName("requireAssignableBranches rechaza un id nulo dentro de la colección")
        void require_assignable_rechaza_id_nulo() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            List<Long> conNulo = new java.util.ArrayList<>();
            conNulo.add(null);
            assertThatThrownBy(() -> authz.requireAssignableBranches(conNulo))
                    .isInstanceOf(BranchAccessDeniedException.class);
        }

        @Test
        @DisplayName("canAccessBranch con id nulo es falso")
        void can_access_branch_id_nulo_es_falso() {
            assertThat(authz.canAccessBranch(null)).isFalse();
        }

        @Test
        @DisplayName("canAccessBranch es verdadero solo si la sede está asignada")
        void can_access_branch_sede_propia() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            assertThat(authz.canAccessBranch(AuthMother.BRANCH_ID)).isTrue();
            assertThat(authz.canAccessBranch(AuthMother.OTHER_BRANCH_ID)).isFalse();
        }

        @Test
        @DisplayName("canAccessBranch de un usuario de sistema siempre es falso")
        void can_access_branch_usuario_de_sistema_es_falso() {
            autenticar(AuthMother.usuarioDeSistema());
            assertThat(authz.canAccessBranch(AuthMother.BRANCH_ID)).isFalse();
        }

        @Test
        @DisplayName("canAccessBranch del proceso interno siempre es falso")
        void can_access_branch_system_context_es_falso() {
            autenticar(SystemContext.INSTANCE);
            assertThat(authz.canAccessBranch(AuthMother.BRANCH_ID)).isFalse();
        }

        @Test
        @DisplayName("canAccessBranch sin autenticación siempre es falso")
        void can_access_branch_sin_autenticacion_es_falso() {
            assertThat(authz.canAccessBranch(AuthMother.BRANCH_ID)).isFalse();
        }

        @Test
        @DisplayName("resolveAccessibleBranch acepta una sede solicitada dentro del alcance")
        void resolve_acepta_sede_solicitada_en_alcance() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID, 20L)));
            assertThat(authz.resolveAccessibleBranch(AuthMother.BRANCH_ID))
                    .isEqualTo(AuthMother.BRANCH_ID);
        }

        @Test
        @DisplayName("resolveAccessibleBranch rechaza una sede solicitada fuera de alcance")
        void resolve_rechaza_sede_fuera_de_alcance() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            assertThatThrownBy(() -> authz.resolveAccessibleBranch(AuthMother.OTHER_BRANCH_ID))
                    .isInstanceOf(BranchAccessDeniedException.class);
        }

        @Test
        @DisplayName("resolveAccessibleBranch sin sede solicitada usa la única sede del empleado")
        void resolve_sin_solicitud_usa_la_unica_sede() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID)));
            assertThat(authz.resolveAccessibleBranch(null)).isEqualTo(AuthMother.BRANCH_ID);
        }

        @Test
        @DisplayName("resolveAccessibleBranch sin sede solicitada y sin sedes asignadas falla")
        void resolve_sin_solicitud_sin_sedes_falla() {
            autenticar(AuthMother.empleado(Set.of(), Set.of()));
            assertThatThrownBy(() -> authz.resolveAccessibleBranch(null))
                    .isInstanceOf(BranchAccessDeniedException.class);
        }

        @Test
        @DisplayName("resolveAccessibleBranch sin sede solicitada y con varias sedes exige elegir")
        void resolve_sin_solicitud_con_varias_sedes_exige_elegir() {
            autenticar(AuthMother.empleado(Set.of(), Set.of(AuthMother.BRANCH_ID, 20L)));
            assertThatThrownBy(() -> authz.resolveAccessibleBranch(null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("branchId");
        }
    }
}
