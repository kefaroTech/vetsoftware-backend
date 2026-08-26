package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ListGlobalMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El catalogo GLOBAL activo, paginado. Ver {@link ListGlobalMedicamentsUseCase}
 * para por que va cerrado a SYSTEM y en que se diferencia de
 * {@code ListMedicamentsService}.
 */
@Observed(name = "medicament.global.list")
@Service
public class ListGlobalMedicamentsService implements ListGlobalMedicamentsUseCase {
    private final MedicamentRepository repository;

    public ListGlobalMedicamentsService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<MedicamentDto> listAll(String q, int page, int pageSize) {
        return repository.findAllGlobal(q, page, pageSize).map(MedicamentDto::from);
    }
}
