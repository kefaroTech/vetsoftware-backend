package com.vetsoftware.app.limitdimension.application.usecase;

import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import com.vetsoftware.app.limitdimension.application.port.in.FindLimitDimensionUseCase;
import com.vetsoftware.app.limitdimension.application.port.out.LimitDimensionRepository;
import com.vetsoftware.app.limitdimension.domain.LimitDimensionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Un eje por su id. */
@Service
public class FindLimitDimensionService implements FindLimitDimensionUseCase {

    private final LimitDimensionRepository repository;

    public FindLimitDimensionService(LimitDimensionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public LimitDimensionDto findById(Long id) {
        return LimitDimensionDto.from(
                repository.findById(id).orElseThrow(() -> new LimitDimensionNotFoundException(id)));
    }
}
