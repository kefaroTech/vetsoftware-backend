package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.port.in.DeleteDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "deworming.delete")
@Service
public class DeleteDewormingService implements DeleteDewormingUseCase {
    private final DewormingRepository repository;

    public DeleteDewormingService(DewormingRepository repository) {
        this.repository = repository;
    }

    /**
     * La comprobacion de existencia va acotada a la empresa: es lo unico que separa
     * un 404 de borrar la desparasitacion de otro tenant. {@code companyId == null}
     * es el camino SYSTEM.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new DewormingNotFoundException(id));
        repository.delete(id);
    }
}
