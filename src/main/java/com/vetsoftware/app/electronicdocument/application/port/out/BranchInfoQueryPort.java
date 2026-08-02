package com.vetsoftware.app.electronicdocument.application.port.out;

import java.util.Optional;

/**
 * Multi-sucursal (B-6): datos de la sede emisora para el bloque {@code point_of_sale} del documento
 * equivalente POS (Res. 000165/2023). El documento lleva {@code branchId}; el proveedor resuelve
 * nombre/código/dirección de esa sede para reflejar el establecimiento real (address) en vez de un
 * valor global.
 */
public interface BranchInfoQueryPort {
  Optional<BranchInfo> findById(Long branchId);

  record BranchInfo(String name, String code, String address) {}
}
