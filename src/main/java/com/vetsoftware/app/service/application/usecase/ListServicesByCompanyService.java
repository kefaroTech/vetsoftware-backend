package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.ListServicesByCompanyUseCase;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.list.by.company")
@org.springframework.stereotype.Service
public class ListServicesByCompanyService implements ListServicesByCompanyUseCase {
    private final ServiceRepository repository;

    public ListServicesByCompanyService(ServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ServiceDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(ServiceDto::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceDto> listDisabledByCompany(Long companyId) {
        // readOnly tx: la query nativa trae los pausados y el mapper hidrata sus asociaciones LAZY aquí dentro.
        return repository.findAllDisabledByCompanyId(companyId).stream().map(ServiceDto::from).toList();
    }
}
