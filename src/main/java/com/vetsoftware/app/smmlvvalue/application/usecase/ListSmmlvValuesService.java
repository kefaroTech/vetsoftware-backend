package com.vetsoftware.app.smmlvvalue.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import com.vetsoftware.app.smmlvvalue.application.port.in.ListSmmlvValuesUseCase;
import com.vetsoftware.app.smmlvvalue.application.port.out.SmmlvValueRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "smmlv.list")
@Service
public class ListSmmlvValuesService implements ListSmmlvValuesUseCase {

    private final SmmlvValueRepository repository;

    public ListSmmlvValuesService(SmmlvValueRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<SmmlvValueDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAll(page, pageSize).map(SmmlvValueDto::from);
    }
}
