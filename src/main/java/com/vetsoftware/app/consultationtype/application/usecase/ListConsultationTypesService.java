package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.ListConsultationTypesUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "consultation.type.list")
@Service
public class ListConsultationTypesService implements ListConsultationTypesUseCase {
    private final ConsultationTypeRepository repository;

    public ListConsultationTypesService(ConsultationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConsultationTypeDto> listAll() {
        return repository.findAll().stream().map(ConsultationTypeDto::from).toList();
    }
}
