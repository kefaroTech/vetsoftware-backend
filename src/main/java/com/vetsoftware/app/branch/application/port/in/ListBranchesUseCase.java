package com.vetsoftware.app.branch.application.port.in;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBranchesUseCase {
  // Multi-sucursal (Fase C): listar las sedes de la PROPIA empresa no requiere un permiso
  // específico —
  // es el insumo del selector de sede que usa cualquier empleado (dato no sensible; sin authz por
  // sede).
  // La gestión (crear/editar) sí está gateada por branch.create/branch.update.
  @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#companyId)")
  List<BranchDto> listAll(Long companyId);
}
