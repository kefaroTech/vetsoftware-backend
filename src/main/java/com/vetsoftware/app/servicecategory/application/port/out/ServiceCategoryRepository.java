package com.vetsoftware.app.servicecategory.application.port.out;

import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository {
    ServiceCategory save(ServiceCategory serviceCategory);
    Optional<ServiceCategory> findById(Long id);

    /** ¿Existe ya una categoría de servicio ACTIVA con este name en la empresa? (unicidad de nombre por empresa) */
    boolean existsByCompanyIdAndName(Long companyId, String name);

    /** Igual, excluyendo la propia categoría (para validar en actualización). */
    boolean existsByCompanyIdAndNameExcludingId(Long companyId, String name, Long id);
    List<ServiceCategory> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
