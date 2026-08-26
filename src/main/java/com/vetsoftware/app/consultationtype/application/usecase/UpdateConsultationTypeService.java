package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.UpdateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNameAlreadyExistsException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.type.update")
@Service
public class UpdateConsultationTypeService implements UpdateConsultationTypeUseCase {
    private final ConsultationTypeRepository repository;

    public UpdateConsultationTypeService(ConsultationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ConsultationTypeDto execute(UpdateConsultationTypeCommand command) {
        ConsultationType consultationType = repository.findById(command.id())
                .orElseThrow(() -> new ConsultationTypeNotFoundException(command.id()));
        // Solo cuentan las filas ACTIVAS, que son las que el indice unico de la base
        // cuenta; una deshabilitada con ese nombre no estorba. Excluye la propia.
        if (repository.existsActiveByNameExcludingId(command.name(), command.id())) {
            throw new ConsultationTypeNameAlreadyExistsException(command.name());
        }
        consultationType.update(command.name(), command.description());
        return ConsultationTypeDto.from(repository.save(consultationType));
    }
}
