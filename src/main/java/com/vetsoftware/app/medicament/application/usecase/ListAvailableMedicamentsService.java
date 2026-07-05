package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ListAvailableMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.list_available")
@Service
public class ListAvailableMedicamentsService implements ListAvailableMedicamentsUseCase {
    private final MedicamentRepository repository;

    public ListAvailableMedicamentsService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicamentDto> listAvailable(Long companyId) {
        return repository.findAllAvailableForCompany(companyId).stream().map(MedicamentDto::from).toList();
    }
}
