package com.vetsoftware.app.testtype.application.usecase;

import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import com.vetsoftware.app.testtype.application.port.in.ListTestTypesUseCase;
import com.vetsoftware.app.testtype.application.port.out.TestTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "test_type.list")
@Service
public class ListTestTypesService implements ListTestTypesUseCase {
    private final TestTypeRepository repository;

    public ListTestTypesService(TestTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TestTypeDto> listAll() {
        return repository.findAll().stream().map(TestTypeDto::from).toList();
    }
}
