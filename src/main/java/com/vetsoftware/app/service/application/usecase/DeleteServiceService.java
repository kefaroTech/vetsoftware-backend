package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.port.in.DeleteServiceUseCase;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.delete")
@org.springframework.stereotype.Service
public class DeleteServiceService implements DeleteServiceUseCase {
    private final ServiceRepository repository;

    public DeleteServiceService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ServiceNotFoundException(id));
        repository.delete(id);
    }
}
