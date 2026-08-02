package com.vetsoftware.app.servicecategory.application.usecase;

import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.ListServiceCategoriesUseCase;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "service.category.list")
@Service
public class ListServiceCategoriesService implements ListServiceCategoriesUseCase {
  private final ServiceCategoryRepository repository;

  public ListServiceCategoriesService(ServiceCategoryRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ServiceCategoryDto> listByCompany(Long companyId) {
    return repository.findAllByCompanyId(companyId).stream().map(ServiceCategoryDto::from).toList();
  }
}
