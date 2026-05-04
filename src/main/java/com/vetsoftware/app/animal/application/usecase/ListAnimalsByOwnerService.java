package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsByOwnerUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "animal.listByOwner")
@Service
public class ListAnimalsByOwnerService implements ListAnimalsByOwnerUseCase {
    private final AnimalRepository repository;

    public ListAnimalsByOwnerService(AnimalRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AnimalDto> listByOwner(Long ownerId, Long companyId) {
        return repository.findByOwnerIdAndCompanyId(ownerId, companyId)
            .stream().map(AnimalDto::from).toList();
    }
}
