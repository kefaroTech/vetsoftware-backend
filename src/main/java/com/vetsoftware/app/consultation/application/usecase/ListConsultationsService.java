package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.in.ListConsultationsUseCase;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "consultation.list")
@Service
public class ListConsultationsService implements ListConsultationsUseCase {
    private final ConsultationRepository repository;

    public ListConsultationsService(ConsultationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConsultationDto> listAll() {
        return repository.findAll().stream().map(ConsultationDto::from).toList();
    }
}
