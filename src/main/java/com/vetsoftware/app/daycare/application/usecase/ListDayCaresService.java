package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "dayCare.list")
@Service
public class ListDayCaresService implements ListDayCaresUseCase {
    private final DayCareRepository repository;

    public ListDayCaresService(DayCareRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DayCareDto> listAll() {
        return repository.findAll().stream().map(DayCareDto::from).toList();
    }
}
