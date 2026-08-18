package com.vetsoftware.app.laboratorytestfile.application.usecase;

import com.vetsoftware.app.laboratorytestfile.application.StorageKeyFactory;
import com.vetsoftware.app.laboratorytestfile.application.command.CreateLaboratoryTestFileCommand;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import com.vetsoftware.app.laboratorytestfile.application.port.in.CreateLaboratoryTestFileUseCase;
import com.vetsoftware.app.laboratorytestfile.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.FileStoragePort;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestFileRepository;
import com.vetsoftware.app.laboratorytestfile.application.port.out.LaboratoryTestQueryPort;
import com.vetsoftware.app.laboratorytestfile.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestFile;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestRef;
import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.file.create")
@Service
public class CreateLaboratoryTestFileService implements CreateLaboratoryTestFileUseCase {
    private final LaboratoryTestFileRepository repository;
    private final LaboratoryTestQueryPort laboratoryTestQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final FileStoragePort fileStoragePort;

    public CreateLaboratoryTestFileService(LaboratoryTestFileRepository repository,
            LaboratoryTestQueryPort laboratoryTestQueryPort, EmployeeQueryPort employeeQueryPort,
            FileStoragePort fileStoragePort) {
        this.repository = repository;
        this.laboratoryTestQueryPort = laboratoryTestQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public LaboratoryTestFileDto execute(CreateLaboratoryTestFileCommand command) {
        LaboratoryTestRef laboratoryTest = laboratoryTestQueryPort
                .findById(command.laboratoryTestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "LaboratoryTest not found: " + command.laboratoryTestId()));
        EmployeeRef uploadedBy = employeeQueryPort.findById(command.uploadedById())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.uploadedById()));

        // El examen ya se confirmo arriba: si la ruta no se resuelve, lo que falta es
        // alguno de los eslabones que la componen (empresa, animal o propietario del
        // animal), no el examen. Reusar el mensaje de "no encontrado" atribuia el
        // fallo a la causa equivocada y mandaba a buscar donde no era.
        LaboratoryTestStoragePathRef storagePath = laboratoryTestQueryPort
                .findStoragePath(command.laboratoryTestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "LaboratoryTest storage path could not be resolved (company, animal or"
                                + " animal owner missing) for laboratoryTestId: "
                                + command.laboratoryTestId()));

        String storageKey = StorageKeyFactory.build(storagePath, command.originalFileName());

        FileStoragePort.StoredFile stored = fileStoragePort.store(storageKey, command.content(),
                command.contentType());

        LaboratoryTestFile file = LaboratoryTestFile.create(stored.key(), stored.bucket(),
                command.originalFileName(), command.contentType(), command.sizeBytes(),
                stored.eTag(), uploadedBy, laboratoryTest);

        return LaboratoryTestFileDto.from(repository.save(file));
    }
}
