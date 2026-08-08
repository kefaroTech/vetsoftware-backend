package com.vetsoftware.app.consultation.application.usecase;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.dto.PageResult;
import com.vetsoftware.app.consultation.application.port.in.ListConsultationsUseCase;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "consultation.list")
@Service
public class ListConsultationsService implements ListConsultationsUseCase {
    private final ConsultationRepository repository;

    public ListConsultationsService(ConsultationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ConsultationDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize).map(ConsultationDto::from);
    }
}
