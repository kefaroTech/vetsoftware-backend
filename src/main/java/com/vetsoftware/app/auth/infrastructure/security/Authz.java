package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authz")
public class Authz {

    /** Permiso comodín: el empleado admin ve/opera en TODA la empresa, sin acotar por sede. */
    private static final String ADMIN_PERMISSION = "admin.all";

    public boolean isMyCompany(Long companyId) {
        if (companyId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
            && auth.getPrincipal() instanceof EmployeeContext me
            && companyId.equals(me.companyId());
    }

    public Long currentCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
            return me.companyId();
        }
        throw new AccessDeniedException("No employee context");
    }

    public Long currentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
            return me.employeeId();
        }
        throw new AccessDeniedException("No employee context");
    }

    /** Verifica que un identificador de empleado provenga del actor autenticado y no haya sido suplantado. */
    public boolean isCurrentEmployee(Long employeeId) {
        if (employeeId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
            && auth.getPrincipal() instanceof EmployeeContext me
            && employeeId.equals(me.employeeId());
    }

    /** Como {@link #currentEmployeeId()} pero devuelve null si no hay contexto de empleado (p.ej. SYSTEM). */
    public Long currentEmployeeIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
            return me.employeeId();
        }
        return null;
    }

    /** ¿El empleado autenticado es admin (permiso {@code admin.all})? El admin no se acota por sede. */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
            && auth.getPrincipal() instanceof EmployeeContext me
            && me.permissions().contains(ADMIN_PERMISSION);
    }

    /** Sedes asignadas al empleado autenticado (para acotar qué sedes puede asignar a otros). */
    public Set<Long> currentBranchIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
            return me.branchIds();
        }
        throw new AccessDeniedException("No employee context");
    }

    /**
     * Exige que el caller pueda ASIGNAR todas esas sedes a un empleado: un admin puede asignar cualquier sede de su
     * empresa; un no-admin (p.ej. un manager con employee.create) solo las sedes que él mismo tiene asignadas.
     * Lanza {@link BranchAccessDeniedException} (→ 403) si alguna sede está fuera de su alcance.
     */
    public void requireAssignableBranches(Collection<Long> branchIds) {
        if (branchIds == null || isAdmin()) return;
        Set<Long> mine = currentBranchIds();
        for (Long id : branchIds) {
            if (id == null || !mine.contains(id)) {
                throw new BranchAccessDeniedException("Branch not assignable by employee: " + id);
            }
        }
    }

    /** ¿El empleado puede operar sobre esta sede? Admin siempre; el resto, solo si la tiene asignada. */
    public boolean canAccessBranch(Long branchId) {
        if (branchId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
            && auth.getPrincipal() instanceof EmployeeContext me
            && (me.permissions().contains(ADMIN_PERMISSION) || me.branchIds().contains(branchId));
    }

    /**
     * Resuelve la sede a aplicar (en escrituras y en filtros de lectura) honrando el alcance del empleado.
     *
     * <ul>
     *   <li><b>Admin</b>: devuelve {@code requested} tal cual — {@code null} significa sin filtro (o que el caso de
     *       uso resuelva la sede activa por defecto de la empresa en escrituras).</li>
     *   <li><b>No-admin con {@code requested} != null</b>: exige que esté en su alcance; si no, lanza
     *       {@link BranchAccessDeniedException} (→ 403 {@code BRANCH_NOT_ALLOWED}).</li>
     *   <li><b>No-admin con {@code requested} == null</b>: si tiene una única sede, la usa (default acotado); si no
     *       tiene ninguna, 403; si tiene varias, {@code IllegalArgumentException} (→ 400) para forzar a elegir.</li>
     * </ul>
     *
     * Nunca deja pasar {@code null} para un no-admin, así una lectura sin {@code branchId} no filtra por toda la
     * empresa (evita fuga de datos de sedes ajenas) y una escritura no cae en la sede "Principal" fuera de alcance.
     */
    public Long resolveAccessibleBranch(Long requested) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof EmployeeContext me)) {
            throw new AccessDeniedException("No employee context");
        }
        if (me.permissions().contains(ADMIN_PERMISSION)) return requested;

        Set<Long> scope = me.branchIds();
        if (requested != null) {
            if (!scope.contains(requested)) {
                throw new BranchAccessDeniedException("Branch not allowed for employee: " + requested);
            }
            return requested;
        }
        if (scope.size() == 1) return scope.iterator().next();
        if (scope.isEmpty()) throw new BranchAccessDeniedException("Employee has no branch assigned");
        throw new IllegalArgumentException("branchId is required");
    }
}
