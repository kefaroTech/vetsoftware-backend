package com.vetsoftware.app.economicactivity.application.usecase;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.ReactivateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "economic.activity.reactivate")
@Service
public class ReactivateEconomicActivityService implements ReactivateEconomicActivityUseCase {
    private final EconomicActivityRepository repository;

    public ReactivateEconomicActivityService(EconomicActivityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EconomicActivityDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new EconomicActivityNotFoundException(id);
        return EconomicActivityDto.from(repository.findById(id)
                .orElseThrow(() -> new EconomicActivityNotFoundException(id)));
    }
}
