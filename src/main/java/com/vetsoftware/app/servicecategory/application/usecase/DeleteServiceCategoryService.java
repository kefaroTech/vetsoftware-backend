package com.vetsoftware.app.servicecategory.application.usecase;

import com.vetsoftware.app.servicecategory.application.port.in.DeleteServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceChildrenQueryPort;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryHasActiveChildrenException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.category.delete")
@Service
public class DeleteServiceCategoryService implements DeleteServiceCategoryUseCase {
    private final ServiceCategoryRepository repository;
    private final ServiceChildrenQueryPort serviceChildrenQueryPort;

    public DeleteServiceCategoryService(
            ServiceCategoryRepository repository,
            ServiceChildrenQueryPort serviceChildrenQueryPort) {
        this.repository = repository;
        this.serviceChildrenQueryPort = serviceChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ServiceCategoryNotFoundException(id));
        if (serviceChildrenQueryPort.existsActiveByServiceCategoryId(id)) {
            throw new ServiceCategoryHasActiveChildrenException(id, "service");
        }
        repository.delete(id);
    }
}
