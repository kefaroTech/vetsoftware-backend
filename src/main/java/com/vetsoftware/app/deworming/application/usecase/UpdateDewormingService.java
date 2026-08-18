package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.UpdateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.deworming.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.deworming.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.AnimalRef;
import com.vetsoftware.app.deworming.domain.CompanyRef;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "deworming.update")
@Service
public class UpdateDewormingService implements UpdateDewormingUseCase {
    private final DewormingRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateDewormingService(DewormingRepository repository, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    /**
     * La carga va acotada a la empresa. El {@code isMyCompany} del puerto solo
     * prueba que el llamante declara SU empresa; con un {@code findById} pelado el
     * efecto no era un rechazo sino una apropiacion: la desparasitacion de otro
     * tenant se reescribia con {@code company} = la del atacante.
     * {@code companyId == null} es el camino SYSTEM.
     */
    @Override
    @Transactional
    public DewormingDto execute(UpdateDewormingCommand command) {
        Deworming deworming = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new DewormingNotFoundException(command.id()));
        // La empresa efectiva: la del command, o la de la fila ya cargada cuando el
        // caller es SYSTEM. Con ella se acotan las dos referencias, que es la fuga que
        // sobrevive a la carga propia ya acotada: no se apropia de la desparasitacion
        // ajena, la cuelga del animal (o la consulta) de otro tenant.
        Long companyId = command.companyId() == null
                ? deworming.getCompany().id()
                : command.companyId();
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

        deworming.update(command.date(), command.lastDeworming(), command.type(), command.product(),
                command.dosage(), command.nextControl(), command.observations(), animal,
                consultation, company);
        return DewormingDto.from(repository.save(deworming));
    }
}
