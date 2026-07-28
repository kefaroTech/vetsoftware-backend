package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.ListNumberingResolutionsUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "numbering.resolution.list")
@Service
public class ListNumberingResolutionsService implements ListNumberingResolutionsUseCase {
    private final NumberingResolutionRepository repository;

    public ListNumberingResolutionsService(NumberingResolutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<NumberingResolutionDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(NumberingResolutionDto::from).toList();
    }
}
