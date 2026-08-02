package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.command.CreateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.in.CreateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationTypeQueryPort;
import com.vetsoftware.app.vaccination.domain.AnimalRef;
import com.vetsoftware.app.vaccination.domain.CompanyRef;
import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.create")
@Service
public class CreateVaccinationService implements CreateVaccinationUseCase {
    private final VaccinationRepository repository;
    private final VaccinationTypeQueryPort vaccinationTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateVaccinationService(VaccinationRepository repository,
            VaccinationTypeQueryPort vaccinationTypeQueryPort, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.vaccinationTypeQueryPort = vaccinationTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public VaccinationDto execute(CreateVaccinationCommand command) {
        VaccinationTypeRef vaccinationType = vaccinationTypeQueryPort
                .findById(command.vaccinationTypeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "VaccinationType not found: " + command.vaccinationTypeId()));
        AnimalRef animal = animalQueryPort.findById(command.animalId()).orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findById(command.consultationId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        Vaccination vaccination = Vaccination.create(command.date(), vaccinationType, command.lot(),
                command.notes(), command.route(), command.applicationSite(),
                command.nextVaccination(), animal, consultation, company);
        return VaccinationDto.from(repository.save(vaccination));
    }
}
