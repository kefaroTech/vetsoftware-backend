package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresByAnimalUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "day.care.list.by.animal")
@Service
public class ListDayCaresByAnimalService implements ListDayCaresByAnimalUseCase {
    private final DayCareRepository repository;

    public ListDayCaresByAnimalService(DayCareRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DayCareDto> listByAnimal(Long animalId, Long companyId, String query,
            int page, int pageSize) {
        return repository.findAllByAnimalIdAndCompanyId(animalId, companyId, query, page, pageSize)
                .map(DayCareDto::from);
    }
}
