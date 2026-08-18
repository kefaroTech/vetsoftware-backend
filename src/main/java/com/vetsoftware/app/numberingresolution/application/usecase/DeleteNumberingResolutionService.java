package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.port.in.DeleteNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "numbering.resolution.delete")
@Service
public class DeleteNumberingResolutionService implements DeleteNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;

    public DeleteNumberingResolutionService(NumberingResolutionRepository repository) {
        this.repository = repository;
    }

    /**
     * {@code companyId} null = caller sin empresa (SYSTEM), que sí puede borrar
     * cualquier resolución; con empresa, la lectura previa va acotada para que
     * borrar la resolución fiscal de otro tenant sea un 404 y no un borrado.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new NumberingResolutionNotFoundException(id));
        repository.delete(id);
    }
}
