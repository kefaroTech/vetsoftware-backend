package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.UpdateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNameAlreadyExistsException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.type.update")
@Service
public class UpdateVaccinationTypeService implements UpdateVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateVaccinationTypeService(VaccinationTypeRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public VaccinationTypeDto execute(UpdateVaccinationTypeCommand command) {
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
        VaccinationType vaccinationType = (command.companyId() == null
                ? repository.findById(command.id()).filter(VaccinationType::isGeneral)
                : repository.findOwnedByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new VaccinationTypeNotFoundException(command.id()));
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
            throw new VaccinationTypeNameAlreadyExistsException(command.name());
        }
        vaccinationType.update(command.name(), command.description(), company, command.general());
        return VaccinationTypeDto.from(repository.save(vaccinationType));
    }
}
