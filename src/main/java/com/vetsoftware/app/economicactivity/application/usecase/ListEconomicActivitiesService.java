package com.vetsoftware.app.economicactivity.application.usecase;

import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.ListEconomicActivitiesUseCase;
import com.vetsoftware.app.economicactivity.application.port.out.EconomicActivityRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "economicActivity.list")
@Service
public class ListEconomicActivitiesService implements ListEconomicActivitiesUseCase {
    private final EconomicActivityRepository repository;

    public ListEconomicActivitiesService(EconomicActivityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<EconomicActivityDto> listAll() {
        return repository.findAll().stream().map(EconomicActivityDto::from).toList();
    }
}
