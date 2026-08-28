package com.vetsoftware.app.limitdimension.application.usecase;

import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import com.vetsoftware.app.limitdimension.application.port.in.ListLimitDimensionsUseCase;
import com.vetsoftware.app.limitdimension.application.port.out.LimitDimensionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El catálogo entero de ejes. Son ocho filas y no crece con el tráfico: no se
 * pagina.
 */
@Service
public class ListLimitDimensionsService implements ListLimitDimensionsUseCase {

    private final LimitDimensionRepository repository;

    public ListLimitDimensionsService(LimitDimensionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LimitDimensionDto> listAll() {
        return repository.findAllOrderedByCode().stream().map(LimitDimensionDto::from).toList();
    }
}
