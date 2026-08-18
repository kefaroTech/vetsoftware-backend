package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.UpdateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.surgery.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.surgery.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.application.port.out.SurgeryTypeQueryPort;
import com.vetsoftware.app.surgery.domain.AnimalRef;
import com.vetsoftware.app.surgery.domain.CompanyRef;
import com.vetsoftware.app.surgery.domain.ConsultationRef;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import com.vetsoftware.app.surgery.domain.SurgeryTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.update")
@Service
public class UpdateSurgeryService implements UpdateSurgeryUseCase {
    private final SurgeryRepository repository;
    private final SurgeryTypeQueryPort surgeryTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateSurgeryService(SurgeryRepository repository,
            SurgeryTypeQueryPort surgeryTypeQueryPort, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.surgeryTypeQueryPort = surgeryTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public SurgeryDto execute(UpdateSurgeryCommand command) {
        // Sin acotar por empresa, el @authz.isMyCompany(#command.companyId) del puerto
        // es vacuo: solo prueba que el atacante declara SU empresa, y el update
        // posterior reescribe el company de la fila ajena — apropiacion, no rechazo.
        Surgery surgery = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new SurgeryNotFoundException(command.id()));
        Long companyId = command.companyId() == null
                ? surgery.getCompany().id()
                : command.companyId();
        // Las referencias entrantes se resuelven acotadas por la MISMA empresa que la
        // fila. Sin eso el update ya no se apropia de nada ajeno, pero si cuelga lo
        // propio de un padre de otro tenant: una cirugia de mi empresa en la historia
        // clinica de la vecina. El tipo va por la variante «general O mia», porque ese
        // catalogo mezcla filas globales con las privadas de cada empresa.
        SurgeryTypeRef surgeryType = surgeryTypeQueryPort
                .findAvailableByIdAndCompanyId(command.surgeryTypeId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "SurgeryType not found: " + command.surgeryTypeId()));
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findByIdAndCompanyId(command.consultationId(), companyId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        surgery.update(command.date(), surgeryType, command.description(), command.medicament(),
                command.observations(), command.complications(), animal, consultation, company);
        return SurgeryDto.from(repository.save(surgery));
    }
}
