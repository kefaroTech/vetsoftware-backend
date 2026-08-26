package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.UpdateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.CompanyRef;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNameAlreadyExistsException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.type.update")
@Service
public class UpdateLaboratoryTestTypeService implements UpdateLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateLaboratoryTestTypeService(LaboratoryTestTypeRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public LaboratoryTestTypeDto execute(UpdateLaboratoryTestTypeCommand command) {
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
        LaboratoryTestType laboratoryTestType = (command.companyId() == null
                ? repository.findById(command.id()).filter(LaboratoryTestType::isGeneral)
                : repository.findOwnedByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new LaboratoryTestTypeNotFoundException(command.id()));
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
            throw new LaboratoryTestTypeNameAlreadyExistsException(command.name());
        }
        laboratoryTestType.update(command.name(), command.description(), company,
                command.general());
        return LaboratoryTestTypeDto.from(repository.save(laboratoryTestType));
    }
}
