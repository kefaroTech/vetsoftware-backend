package com.vetsoftware.app.animal.application.usecase;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.dto.PageResult;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsUseCase;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "animal.list")
@Service
public class ListAnimalsService implements ListAnimalsUseCase {
    private final AnimalRepository repository;

    public ListAnimalsService(AnimalRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<AnimalDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize).map(AnimalDto::from);
    }
}
