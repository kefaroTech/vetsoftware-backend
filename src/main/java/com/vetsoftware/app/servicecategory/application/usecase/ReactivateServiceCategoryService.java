package com.vetsoftware.app.servicecategory.application.usecase;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.ReactivateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_category.reactivate")
@Service
public class ReactivateServiceCategoryService implements ReactivateServiceCategoryUseCase {
    private final ServiceCategoryRepository repository;

    public ReactivateServiceCategoryService(ServiceCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ServiceCategoryDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new ServiceCategoryNotFoundException(id);
        return ServiceCategoryDto.from(repository.findById(id)
            .orElseThrow(() -> new ServiceCategoryNotFoundException(id)));
    }
}
