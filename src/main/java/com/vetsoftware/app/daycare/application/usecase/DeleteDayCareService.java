package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.port.in.DeleteDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "day.care.delete")
@Service
public class DeleteDayCareService implements DeleteDayCareUseCase {
    private final DayCareRepository repository;

    public DeleteDayCareService(DayCareRepository repository) {
        this.repository = repository;
    }

    /**
     * La lectura previa acotada por empresa es lo que convierte un id ajeno en un
     * 404 en vez de en un borrado. Un {@code companyId} nulo es el actor global
     * (SYSTEM), que si puede borrar cualquier fila.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new DayCareNotFoundException(id));
        repository.delete(id);
    }
}
