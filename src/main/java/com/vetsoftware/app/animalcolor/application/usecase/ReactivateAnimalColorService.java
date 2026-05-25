package com.vetsoftware.app.animalcolor.application.usecase;

import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.ReactivateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.out.AnimalColorRepository;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animalcolor.reactivate")
@Service
public class ReactivateAnimalColorService implements ReactivateAnimalColorUseCase {
    private final AnimalColorRepository repository;

    public ReactivateAnimalColorService(AnimalColorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AnimalColorDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new AnimalColorNotFoundException(id);
        return AnimalColorDto.from(repository.findById(id)
            .orElseThrow(() -> new AnimalColorNotFoundException(id)));
    }
}
