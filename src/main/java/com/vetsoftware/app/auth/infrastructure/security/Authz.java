package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.application.dto.SystemContext;
import com.vetsoftware.app.auth.application.dto.SystemUserContext;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component("authz")
public class Authz {

    public static final String COMPANY_SCOPE_HEADER = "X-Company-Id";

    private static final String NO_COMPANY_CONTEXT = "No company context";
    private static final String NO_EMPLOYEE_CONTEXT = "No employee context";
    private static final String NO_SYSTEM_USER_CONTEXT = "No system user context";

    /**
     * Actor autenticado de la request, o {@code null} si no hay ninguno. Único
     * punto de lectura del {@link SecurityContextHolder} de esta clase: todo lo
     * demás decide con un {@code switch} exhaustivo sobre el tipo sellado, de forma
     * que un cuarto actor rompa la compilación aquí y no el comportamiento en
     * producción.
     */
    private static AuthContext currentContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : AuthContext.ofPrincipal(auth.getPrincipal());
    }

    public boolean isMyCompany(Long companyId) {
        if (companyId == null)
            return false;
        return switch (currentContext()) {
            case EmployeeContext me -> companyId.equals(me.companyId());
            case SystemUserContext _ -> false;
            case SystemContext _ -> false;
            case null -> false;
        };
    }

    public Long currentCompanyId() {
        return switch (currentContext()) {
            case EmployeeContext me -> me.companyId();
            case SystemUserContext _ -> requiredSystemCompanyId();
            case SystemContext _ -> throw new AccessDeniedException(NO_COMPANY_CONTEXT);
            case null -> throw new AccessDeniedException(NO_COMPANY_CONTEXT);
        };
    }

    /** Empresa del empleado o {@code null} para un superadmin/proceso global. */
    public Long currentCompanyIdOrNull() {
        return switch (currentContext()) {
            case EmployeeContext me -> me.companyId();
            case SystemUserContext _ -> null;
            case SystemContext _ -> null;
            case null -> null;
        };
    }

    public Long currentEmployeeId() {
        return switch (currentContext()) {
            case EmployeeContext me -> me.employeeId();
            case SystemUserContext _ -> throw new AccessDeniedException(NO_EMPLOYEE_CONTEXT);
            case SystemContext _ -> throw new AccessDeniedException(NO_EMPLOYEE_CONTEXT);
            case null -> throw new AccessDeniedException(NO_EMPLOYEE_CONTEXT);
        };
    }

    /**
     * Verifica que un identificador de empleado provenga del actor autenticado y no
     * haya sido suplantado.
     */
    public boolean isCurrentEmployee(Long employeeId) {
        if (employeeId == null)
            return false;
        return switch (currentContext()) {
            case EmployeeContext me -> employeeId.equals(me.employeeId());
            case SystemUserContext _ -> false;
            case SystemContext _ -> false;
            case null -> false;
        };
    }

    /**
     * Como {@link #currentEmployeeId()} pero devuelve null si no hay contexto de
     * empleado (p.ej. SYSTEM).
     */
    public Long currentEmployeeIdOrNull() {
        return switch (currentContext()) {
            case EmployeeContext me -> me.employeeId();
            case SystemUserContext _ -> null;
            case SystemContext _ -> null;
            case null -> null;
        };
    }

    /**
     * Identificador de la cuenta de plataforma que firma la acción. Existe porque
     * el modelo de suscripciones guarda <strong>quién</strong> tomó una decisión
     * comercial —{@code price_lists.published_by_system_user_id},
     * {@code subscription_amendments.requested_by_system_user_id},
     * {@code subscription_billing_documents.external_registered_by_system_user_id}—
     * y esa firma no la puede elegir el cliente en el cuerpo de la petición.
     *
     * <p>
     * Es el gemelo de {@link #currentEmployeeId()} para el otro actor: sin él, un
     * endpoint {@code SYSTEM} sólo puede recibir el id por el {@code Request}, y
     * entonces la columna deja de probar nada —cualquiera con acceso al endpoint
     * puede atribuirle a otro la publicación de una tarifa—. Un rastro de auditoría
     * que el auditado escribe no es un rastro de auditoría.
     *
     * @throws AccessDeniedException
     *             si el actor no es una cuenta de plataforma
     */
    public Long currentSystemUserId() {
        return switch (currentContext()) {
            case SystemUserContext me -> me.systemUserId();
            case EmployeeContext _ -> throw new AccessDeniedException(NO_SYSTEM_USER_CONTEXT);
            case SystemContext _ -> throw new AccessDeniedException(NO_SYSTEM_USER_CONTEXT);
            case null -> throw new AccessDeniedException(NO_SYSTEM_USER_CONTEXT);
        };
    }

    /**
     * Como {@link #currentSystemUserId()} pero devuelve {@code null} cuando no hay
     * cuenta de plataforma. Para columnas de firma <em>opcionales</em>, nunca para
     * autorizar.
     */
    public Long currentSystemUserIdOrNull() {
        return switch (currentContext()) {
            case SystemUserContext me -> me.systemUserId();
            case EmployeeContext _ -> null;
            case SystemContext _ -> null;
            case null -> null;
        };
    }

    /** El bypass global sólo corresponde a una cuenta de sistema autenticada. */
    public boolean isSuperAdmin() {
        return switch (currentContext()) {
            case SystemUserContext _ -> true;
            case EmployeeContext _ -> false;
            case SystemContext _ -> false;
            case null -> false;
        };
    }

    private static Long requiredSystemCompanyId() {
        if (!(RequestContextHolder
                .getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalArgumentException(
                    COMPANY_SCOPE_HEADER + " is required for tenant operations");
        }
        String raw = attributes.getRequest().getHeader(COMPANY_SCOPE_HEADER);
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    COMPANY_SCOPE_HEADER + " is required for tenant operations");
        }
        try {
            long companyId = Long.parseLong(raw.trim());
            if (companyId <= 0)
                throw new NumberFormatException("non-positive");
            return companyId;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    COMPANY_SCOPE_HEADER + " must be a positive integer");
        }
    }

    /**
     * Sedes asignadas al empleado autenticado (para acotar qué sedes puede asignar
     * a otros).
     */
    public Set<Long> currentBranchIds() {
        return switch (currentContext()) {
            case EmployeeContext me -> me.branchIds();
            case SystemUserContext _ -> throw new AccessDeniedException(NO_EMPLOYEE_CONTEXT);
            case SystemContext _ -> throw new AccessDeniedException(NO_EMPLOYEE_CONTEXT);
            case null -> throw new AccessDeniedException(NO_EMPLOYEE_CONTEXT);
        };
    }

    /**
     * Sedes visibles para el caller, o el conjunto <strong>vacío</strong> cuando no
     * hay contexto de empleado (cuenta de sistema, proceso interno). Variante no
     * lanzadora de {@link #currentBranchIds()}.
     *
     * <p>
     * Existe para <strong>redactar</strong> lo que sale en una respuesta, nunca
     * para autorizar: por eso el caso sin contexto devuelve vacío —no se revela
     * nada— en lugar de interpretarlo como «alcance total». Un superadmin recibe
     * así un mensaje menos detallado, que es el precio correcto de fallar cerrado.
     * Para decidir si una operación se permite, sigue siendo
     * {@link #currentBranchIds()} o {@link #resolveAccessibleBranch(Long)}, que
     * lanzan.
     */
    public Set<Long> currentBranchIdsOrEmpty() {
        return switch (currentContext()) {
            case EmployeeContext me -> me.branchIds() == null ? Set.of() : me.branchIds();
            case SystemUserContext _ -> Set.of();
            case SystemContext _ -> Set.of();
            case null -> Set.of();
        };
    }

    /**
     * Exige que el caller pueda ASIGNAR todas esas sedes a un empleado. El alcance
     * total también se representa con asignaciones explícitas, por lo que nadie
     * obtiene un bypass por código de permiso. Lanza
     * {@link BranchAccessDeniedException} (→ 403) si alguna sede está fuera de su
     * alcance.
     */
    public void requireAssignableBranches(Collection<Long> branchIds) {
        if (branchIds == null)
            return;
        Set<Long> mine = currentBranchIds();
        for (Long id : branchIds) {
            if (id == null || !mine.contains(id)) {
                throw new BranchAccessDeniedException("Branch not assignable by employee: " + id);
            }
        }
    }

    /** ¿El empleado puede operar sobre esta sede? Solo si la tiene asignada. */
    public boolean canAccessBranch(Long branchId) {
        if (branchId == null)
            return false;
        return switch (currentContext()) {
            case EmployeeContext me -> me.branchIds().contains(branchId);
            case SystemUserContext _ -> false;
            case SystemContext _ -> false;
            case null -> false;
        };
    }

    /**
     * Resuelve la sede a aplicar (en escrituras y en filtros de lectura) honrando
     * el alcance del empleado.
     *
     * <ul>
     * <li>Con {@code requested} != null: exige que esté en su alcance; si no, lanza
     * {@link BranchAccessDeniedException} (→ 403 {@code BRANCH_NOT_ALLOWED}).
     * <li>Con {@code requested} == null: si tiene una única sede, la usa (default
     * acotado); si no tiene ninguna, 403; si tiene varias,
     * {@code IllegalArgumentException} (→ 400) para forzar a elegir.
     * </ul>
     *
     * Nunca deja pasar {@code null}, así una lectura sin {@code branchId} no filtra
     * por toda la empresa (evita fuga de datos de sedes ajenas) y una escritura no
     * cae en la sede "Principal" fuera de alcance.
     */
    public Long resolveAccessibleBranch(Long requested) {
        Set<Long> scope = currentBranchIds();
        if (requested != null) {
            if (!scope.contains(requested)) {
                throw new BranchAccessDeniedException(
                        "Branch not allowed for employee: " + requested);
            }
            return requested;
        }
        if (scope.size() == 1)
            return scope.iterator().next();
        if (scope.isEmpty())
            throw new BranchAccessDeniedException("Employee has no branch assigned");
        throw new IllegalArgumentException("branchId is required");
    }
}
