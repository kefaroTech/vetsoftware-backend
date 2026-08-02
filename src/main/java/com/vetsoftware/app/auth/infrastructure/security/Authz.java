package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
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
    if (auth != null && auth.getPrincipal() instanceof SystemUserContext) {
      return requiredSystemCompanyId();
    }
    throw new AccessDeniedException("No company context");
  }

  /** Empresa del empleado o {@code null} para un superadmin/proceso global. */
  public Long currentCompanyIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
      return me.companyId();
    }
    return null;
  }

  public Long currentEmployeeId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
      return me.employeeId();
    }
    throw new AccessDeniedException("No employee context");
  }

  /**
   * Verifica que un identificador de empleado provenga del actor autenticado y no haya sido
   * suplantado.
   */
  public boolean isCurrentEmployee(Long employeeId) {
    if (employeeId == null) return false;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getPrincipal() instanceof EmployeeContext me
        && employeeId.equals(me.employeeId());
  }

  /**
   * Como {@link #currentEmployeeId()} pero devuelve null si no hay contexto de empleado (p.ej.
   * SYSTEM).
   */
  public Long currentEmployeeIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof EmployeeContext me) {
      return me.employeeId();
    }
    return null;
  }

  /** El bypass global sólo corresponde a una cuenta de sistema autenticada. */
  public boolean isSuperAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof SystemUserContext;
  }

  private static Long requiredSystemCompanyId() {
    if (!(RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes)) {
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
      if (companyId <= 0) throw new NumberFormatException("non-positive");
      return companyId;
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(COMPANY_SCOPE_HEADER + " must be a positive integer");
    }
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
   * Exige que el caller pueda ASIGNAR todas esas sedes a un empleado. El alcance total también se
   * representa con asignaciones explícitas, por lo que nadie obtiene un bypass por código de
   * permiso. Lanza {@link BranchAccessDeniedException} (→ 403) si alguna sede está fuera de su
   * alcance.
   */
  public void requireAssignableBranches(Collection<Long> branchIds) {
    if (branchIds == null) return;
    Set<Long> mine = currentBranchIds();
    for (Long id : branchIds) {
      if (id == null || !mine.contains(id)) {
        throw new BranchAccessDeniedException("Branch not assignable by employee: " + id);
      }
    }
  }

  /** ¿El empleado puede operar sobre esta sede? Solo si la tiene asignada. */
  public boolean canAccessBranch(Long branchId) {
    if (branchId == null) return false;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getPrincipal() instanceof EmployeeContext me
        && me.branchIds().contains(branchId);
  }

  /**
   * Resuelve la sede a aplicar (en escrituras y en filtros de lectura) honrando el alcance del
   * empleado.
   *
   * <ul>
   *   <li>Con {@code requested} != null: exige que esté en su alcance; si no, lanza {@link
   *       BranchAccessDeniedException} (→ 403 {@code BRANCH_NOT_ALLOWED}).
   *   <li>Con {@code requested} == null: si tiene una única sede, la usa (default acotado); si no
   *       tiene ninguna, 403; si tiene varias, {@code IllegalArgumentException} (→ 400) para forzar
   *       a elegir.
   * </ul>
   *
   * Nunca deja pasar {@code null}, así una lectura sin {@code branchId} no filtra por toda la
   * empresa (evita fuga de datos de sedes ajenas) y una escritura no cae en la sede "Principal"
   * fuera de alcance.
   */
  public Long resolveAccessibleBranch(Long requested) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth != null && auth.getPrincipal() instanceof EmployeeContext me)) {
      throw new AccessDeniedException("No employee context");
    }
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
