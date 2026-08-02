package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.SearchServicesUseCase;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import io.micrometer.observation.annotation.Observed;

@Observed(name = "service.search")
@org.springframework.stereotype.Service
public class SearchServicesService implements SearchServicesUseCase {
  private final ServiceRepository repository;

  public SearchServicesService(ServiceRepository repository) {
    this.repository = repository;
  }

  @Override
  public PageResult<ServiceDto> execute(SearchServicesCommand command) {
    return repository.search(command).map(ServiceDto::from);
  }
}
