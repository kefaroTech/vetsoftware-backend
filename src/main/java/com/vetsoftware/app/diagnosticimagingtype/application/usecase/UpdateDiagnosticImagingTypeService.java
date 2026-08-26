package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNameAlreadyExistsException;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "diagnostic.imaging.type.update")
@Service
public class UpdateDiagnosticImagingTypeService implements UpdateDiagnosticImagingTypeUseCase {
    private final DiagnosticImagingTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateDiagnosticImagingTypeService(DiagnosticImagingTypeRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public DiagnosticImagingTypeDto execute(UpdateDiagnosticImagingTypeCommand command) {
        // El @PreAuthorize solo prueba que el caller declara SU empresa; sin acotar
        // aqui la lectura, un id ajeno se cargaba y el update posterior reescribia su
        // company: el tipo de otro tenant pasaba a tener el company_id del atacante.
        // Finder ESTRICTO a proposito: una fila general tampoco se puede apropiar.
        // El camino SYSTEM alcanza SOLO el catalogo de plataforma. El .filter no es
        // defensa en profundidad: es la barrera. Desde que el controller pasa
        // currentCompanyIdOrNull() (#565) este ternario es alcanzable de verdad, y sin
        // el filtro un PUT de plataforma con el id de una fila PRIVADA la cargaba y el
        // update posterior le ponia company = null y general = true: el tipo privado de
        // una clinica pasaba en silencio al catalogo global, visible para todos los
        // tenants. Es el espejo del motivo por el que el camino del empleado usa el
        // finder de lo PROPIO. Un 404 y no un 403: no se revela de quien es la fila.
        DiagnosticImagingType type = (command.companyId() == null
                ? repository.findById(command.id()).filter(DiagnosticImagingType::isGeneral)
                : repository.findOwnedByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new DiagnosticImagingTypeNotFoundException(command.id()));
        CompanyRef company = command.companyId() == null
                ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company not found: " + command.companyId()));
        // La unicidad del nombre se comprueba en el AMBITO al que la fila va a
        // quedar: la empresa del command, o el catalogo de plataforma cuando el
        // camino es SYSTEM (companyId nulo). Solo cuentan las ACTIVAS, que son las
        // que el indice unico de la base cuenta.
        if (repository.existsActiveByNameAndCompanyIdExcludingId(command.name(),
                command.companyId(), command.id())) {
            throw new DiagnosticImagingTypeNameAlreadyExistsException(command.name());
        }
        type.update(command.name(), command.description(), company, command.general());
        return DiagnosticImagingTypeDto.from(repository.save(type));
    }
}
