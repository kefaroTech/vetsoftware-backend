package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.UpdateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.CompanyRef;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
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
        VaccinationType vaccinationType = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findOwnedByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new VaccinationTypeNotFoundException(command.id()));
        CompanyRef company = command.companyId() == null
                ? null
                : companyQueryPort.findById(command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Company not found: " + command.companyId()));
        vaccinationType.update(command.name(), command.description(), company, command.general());
        return VaccinationTypeDto.from(repository.save(vaccinationType));
    }
}
