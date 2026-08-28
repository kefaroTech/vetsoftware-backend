package com.vetsoftware.app.smmlvvalue.application.usecase;

import com.vetsoftware.app.smmlvvalue.application.command.ChangeSmmlvStatusCommand;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import com.vetsoftware.app.smmlvvalue.application.port.in.ChangeSmmlvStatusUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.out.SmmlvValueRepository;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValue;
import com.vetsoftware.app.smmlvvalue.domain.SmmlvValueNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota la suspension —o su levantamiento— sobre el ano que ya existe.
 *
 * <p>
 * {@code @Transactional} porque son dos operaciones de repositorio, lectura y
 * escritura, y es justo el ciclo leer-modificar-guardar que el {@code @Version}
 * de esta entidad protege: dos operadores registrando providencias distintas
 * sobre el mismo ano se pisarian sin ruido.
 */
@Observed(name = "smmlv.status")
@Service
public class ChangeSmmlvStatusService implements ChangeSmmlvStatusUseCase {

    private final SmmlvValueRepository repository;

    public ChangeSmmlvStatusService(SmmlvValueRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SmmlvValueDto execute(ChangeSmmlvStatusCommand command) {
        SmmlvValue value = repository.findByFiscalYear(command.fiscalYear())
                .orElseThrow(() -> new SmmlvValueNotFoundException(command.fiscalYear()));
        value.changeStatus(command.status(), command.statusReference(), command.statusChangedOn());
        return SmmlvValueDto.from(repository.save(value));
    }
}
