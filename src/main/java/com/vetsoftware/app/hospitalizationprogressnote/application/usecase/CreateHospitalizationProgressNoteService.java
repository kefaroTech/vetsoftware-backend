package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.CreateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.progress.note.create")
@Service
public class CreateHospitalizationProgressNoteService
        implements
            CreateHospitalizationProgressNoteUseCase {
    private final HospitalizationProgressNoteRepository repository;
    private final HospitalizationQueryPort hospitalizationQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public CreateHospitalizationProgressNoteService(
            HospitalizationProgressNoteRepository repository,
            HospitalizationQueryPort hospitalizationQueryPort,
            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.hospitalizationQueryPort = hospitalizationQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    public HospitalizationProgressNoteDto execute(
            CreateHospitalizationProgressNoteCommand command) {
        // Referencia acotada: el companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId()), asi que nunca es null aqui. Una hospitalizacion
        // de
        // otro tenant no se resuelve y la creacion falla con el mismo "not found" que
        // un
        // id inexistente: quien lo intenta no llega a saber si la fila existe.
        HospitalizationRef hospitalization = hospitalizationQueryPort
                .findByIdAndCompanyId(command.hospitalizationId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hospitalization not found: " + command.hospitalizationId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById()).orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        HospitalizationProgressNote progressNote = HospitalizationProgressNote
                .create(command.description(), hospitalization, createdBy);
        return HospitalizationProgressNoteDto.from(repository.save(progressNote));
    }
}
