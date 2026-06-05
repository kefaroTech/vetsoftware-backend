package com.vetsoftware.app.servicecategory.application.port.out;

import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository {
    ServiceCategory save(ServiceCategory serviceCategory);
    Optional<ServiceCategory> findById(Long id);
    List<ServiceCategory> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id);
}
