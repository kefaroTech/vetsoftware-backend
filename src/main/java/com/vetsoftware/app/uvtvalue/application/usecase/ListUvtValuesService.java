package com.vetsoftware.app.uvtvalue.application.usecase;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import com.vetsoftware.app.uvtvalue.application.port.in.ListUvtValuesUseCase;
import com.vetsoftware.app.uvtvalue.application.port.out.UvtValueRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "uvt.list")
@Service
public class ListUvtValuesService implements ListUvtValuesUseCase {

    private final UvtValueRepository repository;

    public ListUvtValuesService(UvtValueRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<UvtValueDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAll(page, pageSize).map(UvtValueDto::from);
    }
}
