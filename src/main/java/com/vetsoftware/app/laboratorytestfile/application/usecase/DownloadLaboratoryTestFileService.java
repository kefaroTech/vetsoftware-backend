package com.vetsoftware.app.laboratorytestfile.application.usecase;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.DownloadLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory_test_file.download")
@Service
public class DownloadLaboratoryTestFileService implements DownloadLaboratoryTestFileUseCase {
    private final LaboratoryTestFileRepository repository;
    private final FileStoragePort fileStoragePort;

    public DownloadLaboratoryTestFileService(LaboratoryTestFileRepository repository,
                                             FileStoragePort fileStoragePort) {
        this.repository = repository;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public LaboratoryTestFileDownloadDto download(Long id) {
        LaboratoryTestFile file = repository.findById(id)
            .orElseThrow(() -> new LaboratoryTestFileNotFoundException(id));
        byte[] content = fileStoragePort.retrieve(file.getStorageKey());
        return new LaboratoryTestFileDownloadDto(file.getOriginalFileName(), file.getContentType(), content);
    }
}
