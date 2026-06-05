package com.vetsoftware.app.service.application.port.out;

import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import java.util.Optional;

public interface ServiceCategoryQueryPort {
    Optional<ServiceCategoryRef> findById(Long serviceCategoryId);
}
