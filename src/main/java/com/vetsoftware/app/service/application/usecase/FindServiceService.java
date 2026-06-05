package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.FindServiceUseCase;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import io.micrometer.observation.annotation.Observed;

@Observed(name = "service.find")
@org.springframework.stereotype.Service
public class FindServiceService implements FindServiceUseCase {
    private final ServiceRepository repository;

    public FindServiceService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public ServiceDto findById(Long id) {
        return repository.findById(id)
            .map(ServiceDto::from)
            .orElseThrow(() -> new ServiceNotFoundException(id));
    }
}
