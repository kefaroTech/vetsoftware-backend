package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.FindDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "deworming.find")
@Service
public class FindDewormingService implements FindDewormingUseCase {
    private final DewormingRepository repository;

    public FindDewormingService(DewormingRepository repository) {
        this.repository = repository;
    }

    @Override
    public DewormingDto findById(Long id) {
        return DewormingDto.from(repository.findById(id)
            .orElseThrow(() -> new DewormingNotFoundException(id)));
    }
}
