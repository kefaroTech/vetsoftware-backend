package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.ReactivateServiceUseCase;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.ServiceUsageLimitPort;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.reactivate")
@org.springframework.stereotype.Service
public class ReactivateServiceService implements ReactivateServiceUseCase {
    private final ServiceRepository repository;
    private final ServiceUsageLimitPort usageLimitPort;

    public ReactivateServiceService(ServiceRepository repository,
            ServiceUsageLimitPort usageLimitPort) {
        this.repository = repository;
        this.usageLimitPort = usageLimitPort;
    }

    @Override
    @Transactional
    public ServiceDto execute(Long id, Long companyId) {
        usageLimitPort.checkNotExceeded(companyId);
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new ServiceNotFoundException(id);
        return ServiceDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ServiceNotFoundException(id)));
    }
}
