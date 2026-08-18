package com.vetsoftware.app.laboratorytestfile.application.usecase;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.DownloadLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.file.download")
@Service
public class DownloadLaboratoryTestFileService implements DownloadLaboratoryTestFileUseCase {
    private final LaboratoryTestFileRepository repository;
    private final FileStoragePort fileStoragePort;

    public DownloadLaboratoryTestFileService(LaboratoryTestFileRepository repository,
            FileStoragePort fileStoragePort) {
        this.repository = repository;
        this.fileStoragePort = fileStoragePort;
    }

    /**
     * La lectura acotada por empresa es lo unico que separa a un empleado del PDF
     * de otro tenant: si el archivo no es de su empresa el metodo lanza antes de
     * tocar S3, asi que ni siquiera se llega a leer el objeto. Un {@code companyId}
     * nulo es el actor global (SYSTEM), que si puede leer cualquier fila.
     */
    @Override
    public LaboratoryTestFileDownloadDto download(Long id, Long companyId) {
        LaboratoryTestFile file = (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new LaboratoryTestFileNotFoundException(id));
        byte[] content = fileStoragePort.retrieve(file.getStorageKey());
        return new LaboratoryTestFileDownloadDto(file.getOriginalFileName(), file.getContentType(),
                content);
    }
}
