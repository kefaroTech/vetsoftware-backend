package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.FindMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.find")
@Service
public class FindMedicamentService implements FindMedicamentUseCase {
    private final MedicamentRepository repository;

    public FindMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public MedicamentDto findById(Long id) {
        return MedicamentDto.from(repository.findById(id)
                .orElseThrow(() -> new MedicamentNotFoundException(id)));
    }
}
