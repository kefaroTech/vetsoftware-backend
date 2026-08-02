package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.ReactivateServiceUseCase;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.reactivate")
@org.springframework.stereotype.Service
public class ReactivateServiceService implements ReactivateServiceUseCase {
  private final ServiceRepository repository;

  public ReactivateServiceService(ServiceRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public ServiceDto execute(Long id, Long companyId) {
    int rows = repository.reactivate(id, companyId);
    if (rows == 0) throw new ServiceNotFoundException(id);
    return ServiceDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ServiceNotFoundException(id)));
  }
}
