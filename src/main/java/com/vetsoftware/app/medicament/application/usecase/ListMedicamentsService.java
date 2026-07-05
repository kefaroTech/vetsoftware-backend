package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ListMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.list")
@Service
public class ListMedicamentsService implements ListMedicamentsUseCase {
    private final MedicamentRepository repository;

    public ListMedicamentsService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicamentDto> listAll() {
        return repository.findAll().stream().map(MedicamentDto::from).toList();
    }
}
