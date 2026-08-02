package com.vetsoftware.app.supplier.application.port.out;

import com.vetsoftware.app.supplier.application.command.SearchSuppliersCommand;
import com.vetsoftware.app.supplier.application.dto.PageResult;
import com.vetsoftware.app.supplier.domain.Supplier;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
  Supplier save(Supplier supplier);

  Optional<Supplier> findById(Long id);

  Optional<Supplier> findByIdAndCompanyId(Long id, Long companyId);

  /**
   * ¿Existe ya un proveedor ACTIVO con este name en la empresa? (unicidad de nombre por empresa)
   */
  boolean existsByCompanyIdAndName(Long companyId, String name);

  /** Igual, excluyendo el propio proveedor (para validar en actualización). */
  boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id);

  List<Supplier> findAll();

  List<Supplier> findAllByCompanyId(Long companyId);

  /** Proveedores PAUSADOS (enabled=false) de la empresa, para el listado de reactivación. */
  List<Supplier> findAllDisabledByCompanyId(Long companyId);

  PageResult<Supplier> search(SearchSuppliersCommand command);

  void delete(Long id);

  int reactivate(Long id, Long companyId);
}
