package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ListDisabledGlobalMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Los globales pausados, que son los unicos reactivables. Ver
 * {@link ListDisabledGlobalMedicamentsUseCase}.
 */
@Observed(name = "medicament.global.list.disabled")
@Service
public class ListDisabledGlobalMedicamentsService implements ListDisabledGlobalMedicamentsUseCase {
    private final MedicamentRepository repository;

    public ListDisabledGlobalMedicamentsService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MedicamentDto> listDisabled() {
        return repository.findAllDisabledGlobal().stream().map(MedicamentDto::from).toList();
    }
}
