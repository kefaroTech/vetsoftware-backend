package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.port.in.DeleteSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.delete")
@Service
public class DeleteSurgeryService implements DeleteSurgeryUseCase {
    private final SurgeryRepository repository;

    public DeleteSurgeryService(SurgeryRepository repository) {
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
                .orElseThrow(() -> new SurgeryNotFoundException(id));
        repository.delete(id);
    }
}
