package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ListDisabledMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.list.disabled")
@Service
public class ListDisabledMedicamentsService implements ListDisabledMedicamentsUseCase {
    private final MedicamentRepository repository;

    public ListDisabledMedicamentsService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicamentDto> listDisabled(Long companyId) {
        return repository.findAllDisabledForCompany(companyId).stream().map(MedicamentDto::from).toList();
    }
}
