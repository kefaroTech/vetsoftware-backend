package com.vetsoftware.app.servicecategory.application.usecase;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.FindServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "service_category.find")
@Service
public class FindServiceCategoryService implements FindServiceCategoryUseCase {
    private final ServiceCategoryRepository repository;

    public FindServiceCategoryService(ServiceCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public ServiceCategoryDto findById(Long id) {
        return ServiceCategoryDto.from(repository.findById(id)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(id)));
    }
}
