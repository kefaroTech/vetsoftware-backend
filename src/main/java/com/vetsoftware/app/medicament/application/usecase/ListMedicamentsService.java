package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicament.application.port.in.ListMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.list")
@Service
public class ListMedicamentsService implements ListMedicamentsUseCase {
    private final MedicamentRepository repository;

    public ListMedicamentsService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<MedicamentDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(MedicamentDto::from);
    }
}
