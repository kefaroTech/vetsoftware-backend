package com.vetsoftware.app.tax.application.port.out;

import com.vetsoftware.app.tax.domain.Tax;
import java.util.List;
import java.util.Optional;

public interface TaxRepository {
    Tax save(Tax tax);
    Optional<Tax> findById(Long id);
    Optional<Tax> findByIdAndCompanyId(Long id, Long companyId);

    /** ¿Existe ya un impuesto ACTIVO con este name en la empresa? (unicidad de nombre por empresa) */
    boolean existsByCompanyIdAndName(Long companyId, String name);

    /** Igual, excluyendo el propio impuesto (para validar en actualización). */
    boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id);
    List<Tax> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id, Long companyId);
}
