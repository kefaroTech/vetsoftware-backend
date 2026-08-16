package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.dto.PageResult;
import com.vetsoftware.app.spa.application.port.in.ListSpasByAnimalUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "spa.list.by.animal")
@Service
public class ListSpasByAnimalService implements ListSpasByAnimalUseCase {
    private final SpaRepository repository;

    public ListSpasByAnimalService(SpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SpaDto> listByAnimal(Long animalId, Long companyId, String query, int page,
            int pageSize) {
        return repository.findAllByAnimalIdAndCompanyId(animalId, companyId, query, page, pageSize)
                .map(SpaDto::from);
    }
}
