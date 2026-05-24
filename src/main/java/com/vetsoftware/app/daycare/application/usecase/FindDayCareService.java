package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.FindDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "dayCare.find")
@Service
public class FindDayCareService implements FindDayCareUseCase {
    private final DayCareRepository repository;

    public FindDayCareService(DayCareRepository repository) {
        this.repository = repository;
    }

    @Override
    public DayCareDto findById(Long id) {
        return DayCareDto.from(repository.findById(id)
            .orElseThrow(() -> new DayCareNotFoundException(id)));
    }
}
