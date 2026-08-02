package com.vetsoftware.app.registration.application.port.out;

/**
 * Crea la sucursal inicial ("Principal") de la empresa recién registrada, para que toda empresa
 * tenga siempre ≥1 sede (invariante del multi-sucursal). Espejo del backfill que sembró las
 * empresas existentes.
 */
public interface BranchCreator {
  void create(String name, String code, String address, String phone, Long cityId, Long companyId);
}
