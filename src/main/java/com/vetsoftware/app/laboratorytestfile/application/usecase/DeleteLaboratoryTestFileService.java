package com.vetsoftware.app.laboratorytestfile.application.usecase;

import com.vetsoftware.app.laboratorytestfile.application.port.in.DeleteLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory_test_file.delete")
@Service
public class DeleteLaboratoryTestFileService implements DeleteLaboratoryTestFileUseCase {
    private final LaboratoryTestFileRepository repository;
    private final FileStoragePort fileStoragePort;

    public DeleteLaboratoryTestFileService(LaboratoryTestFileRepository repository,
                                           FileStoragePort fileStoragePort) {
        this.repository = repository;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        LaboratoryTestFile file = repository.findById(id)
            .orElseThrow(() -> new LaboratoryTestFileNotFoundException(id));
        // se borra primero la fila (dentro de la tx) y luego el objeto en S3:
        // si S3 falla, la tx hace rollback y la metadata se conserva.
        repository.delete(id);
        fileStoragePort.delete(file.getStorageKey());
    }
}
