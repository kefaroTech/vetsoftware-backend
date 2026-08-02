package com.vetsoftware.app.laboratorytestfile.application.usecase;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.ListLaboratoryTestFilesByLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.file.list.by.laboratory.test")
@Service
public class ListLaboratoryTestFilesByLaboratoryTestService
        implements
            ListLaboratoryTestFilesByLaboratoryTestUseCase {
    private final LaboratoryTestFileRepository repository;

    public ListLaboratoryTestFilesByLaboratoryTestService(LaboratoryTestFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LaboratoryTestFileDto> listByLaboratoryTest(Long laboratoryTestId) {
        return repository.findAllByLaboratoryTestId(laboratoryTestId).stream()
                .map(LaboratoryTestFileDto::from).toList();
    }
}
