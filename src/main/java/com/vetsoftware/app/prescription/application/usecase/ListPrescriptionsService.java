package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.prescription.application.port.in.ListPrescriptionsUseCase;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "prescription.list")
@Service
public class ListPrescriptionsService implements ListPrescriptionsUseCase {
    private final PrescriptionRepository repository;

    public ListPrescriptionsService(PrescriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PrescriptionDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(PrescriptionDto::from);
    }
}
