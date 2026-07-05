package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ReactivateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.reactivate")
@Service
public class ReactivateMedicamentService implements ReactivateMedicamentUseCase {
    private final MedicamentRepository repository;

    public ReactivateMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MedicamentDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new MedicamentNotFoundException(id);
        return MedicamentDto.from(repository.findById(id)
                .orElseThrow(() -> new MedicamentNotFoundException(id)));
    }
}
