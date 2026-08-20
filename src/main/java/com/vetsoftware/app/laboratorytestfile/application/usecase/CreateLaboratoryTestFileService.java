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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Crea el adjunto de un examen de laboratorio: sube el contenido al
 * almacenamiento de objetos y despues graba la fila que lo referencia.
 *
 * <p>
 * <b>Esta clase NO lleva {@code @Transactional}, y es deliberado.</b> La regla
 * dura {@code SIN_IO_EXTERNO_EN_TRANSACCION} de
 * {@code HexagonalArchitectureTest} prohibe hacer I/O externo dentro de una
 * transaccion, y con razon: una subida lenta reteniendo una conexion del pool
 * es peor problema que el que se intenta evitar. No "arregles" este servicio
 * envolviendolo en una transaccion — romperia el build y el pool.
 *
 * <p>
 * La otra mitad de ese patron es la compensacion, que si vive aqui: si el
 * {@code save} falla despues de que el objeto ya subio, se borra el objeto y se
 * relanza el fallo original. Es <i>best effort</i>: si el proceso muere entre
 * la subida y el {@code catch}, el huerfano queda igual (cubrir eso exigiria
 * una tabla de pendientes y un job de barrido, fuera de alcance).
 */
@Observed(name = "laboratory.test.file.create")
@Service
public class CreateLaboratoryTestFileService implements CreateLaboratoryTestFileUseCase {
    private static final Logger log = LoggerFactory
            .getLogger(CreateLaboratoryTestFileService.class);

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

        // El objeto YA esta en S3. Si la fila no se graba (FK rota porque
        // borraron el examen mientras se subia, base caida, timeout) el objeto
        // se queda sin nadie que lo referencie. Y lo que queda ahi no es un byte
        // cualquiera: son datos clinicos de un paciente fuera de todo
        // inventario, invisibles para cualquier proceso de borrado — incluido el
        // borrado a peticion del titular, que solo puede borrar lo que la base
        // de datos conoce. Por eso se compensa aqui mismo.
        try {
            return LaboratoryTestFileDto.from(repository.save(file));
        } catch (RuntimeException e) {
            compensarSubida(stored.key(), e);
            throw e;
        }
    }

    /**
     * Borra el objeto recien subido tras un fallo al grabar la fila. Nunca lanza:
     * el fallo del borrado es secundario y no puede tapar la excepcion original,
     * que es la que explica por que fallo la operacion y la que el cliente tiene
     * que ver. Si el borrado falla, se adjunta como {@code suppressed} de la
     * original y se registra la clave del objeto, que es el unico rastro con el que
     * despues se puede encontrar el huerfano en el bucket.
     */
    private void compensarSubida(String storageKey, RuntimeException falloOriginal) {
        try {
            fileStoragePort.delete(storageKey);
            // A nivel WARN y no INFO: no es el curso normal, y su frecuencia es el dato
            // que decide si algun dia hace falta la tabla de pendientes + job de barrido.
            log.warn("Se borró el adjunto de laboratorio {} tras fallar la escritura en base"
                    + " de datos", storageKey);
        } catch (RuntimeException falloDelBorrado) {
            falloOriginal.addSuppressed(falloDelBorrado);
            log.error("Adjunto de laboratorio huérfano en el bucket: falló el borrado"
                    + " compensatorio de {}. Son datos clínicos que ninguna fila referencia,"
                    + " así que ningún proceso de borrado los alcanza; hay que eliminarlo a"
                    + " mano.", storageKey, falloDelBorrado);
        }
    }
}
