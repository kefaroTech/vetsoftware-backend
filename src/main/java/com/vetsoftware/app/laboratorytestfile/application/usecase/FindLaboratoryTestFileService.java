package com.vetsoftware.app.laboratorytestfile.application.usecase;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.FindLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.file.find")
@Service
public class FindLaboratoryTestFileService implements FindLaboratoryTestFileUseCase {
    private final LaboratoryTestFileRepository repository;

    public FindLaboratoryTestFileService(LaboratoryTestFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public LaboratoryTestFileDto findById(Long id) {
        return LaboratoryTestFileDto.from(repository.findById(id)
            .orElseThrow(() -> new LaboratoryTestFileNotFoundException(id)));
    }
}
