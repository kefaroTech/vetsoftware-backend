package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.type.create")
@Service
public class CreateDiagnosticImagingTypeService implements CreateDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    /**
     * El alta comprueba que el nombre esté libre DENTRO DE SU ÁMBITO —la empresa
     * para un tipo propio, el catálogo de plataforma para uno global— y, si lo
     * ocupa una fila dada de baja, la reactiva en vez de insertar otra.
     *
     * <p>
     * Sin la guarda el choque lo detectaba solo la base y salía como un 409 con
     * {@code "Database constraint violation"}: en inglés, sin nombrar el campo y
     * sin código de negocio, así que el formulario no podía marcar {@code name} en
     * rojo (#559). Sin la rama de reactivación el mensaje era además engañoso: la
     * fila que ocupaba el nombre estaba deshabilitada, la usuaria no la veía en el
     * listado —{@code @SQLRestriction("enabled = true")}— y el sistema le hablaba
     * de un conflicto con algo que para ella no existía. La baja lógica LIBERA el
     * nombre (el índice único cubre solo las activas), así que reactivar es la
     * respuesta correcta y no un apaño (#432).
     *
     * <p>
     * La guarda NO sustituye al mapeo de la constraint en el handler: no cierra la
     * carrera entre dos altas simultáneas, que es lo que documentó #437.
     */
    @Override
    @Transactional
    public DiagnosticImagingTypeDto execute(CreateDiagnosticImagingTypeCommand command) {
        CompanyRef company = command.companyId() == null
                ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company not found: " + command.companyId()));
        Optional<DiagnosticImagingType> existing = repository
                .findByNameAndCompanyIdIncludingDisabled(command.name(), command.companyId());
        if (existing.isPresent()) {
            return DiagnosticImagingTypeDto.from(reactivate(existing.get(), command, company));
        }
        return DiagnosticImagingTypeDto.from(repository.save(DiagnosticImagingType
                .create(command.name(), command.description(), company, command.general())));
    }

    /**
     * Rama de reactivación. El {@code update} del dominio va ANTES del UPDATE
     * nativo a propósito: valida el XOR general/empresa y aborta sin haber
     * resucitado nada si el alta es incoherente.
     */
    private DiagnosticImagingType reactivate(DiagnosticImagingType existing,
            CreateDiagnosticImagingTypeCommand command, CompanyRef company) {
        if (existing.isEnabled()) {
            throw new DiagnosticImagingTypeNameAlreadyExistsException(command.name());
        }
        existing.update(command.name(), command.description(), company, command.general());
        existing.enable();
        int filas = repository.reactivateWithDetails(existing.getId(), command.companyId(),
                command.name(), command.description());
        // La fila estaba ahi cuando la leimos, en ESTA misma transaccion: que el UPDATE
        // no alcance ninguna significa que otra operacion la borro o le cambio el dueno
        // entre medias. Devolver el DTO igualmente afirmaria un enabled = true que no
        // esta en la base -el fallo silencioso que la baja logica hace dificil de ver-.
        // Mismo codigo que el candado optimista (409 CONCURRENT_MODIFICATION) porque
        // para el front la accion es la misma: recargar y reintentar.
        if (filas == 0) {
            throw new ObjectOptimisticLockingFailureException(DiagnosticImagingType.class,
                    existing.getId());
        }
        return existing;
    }
}
