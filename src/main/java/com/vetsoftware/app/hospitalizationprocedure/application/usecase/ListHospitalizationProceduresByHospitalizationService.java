package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ListHospitalizationProceduresByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization_procedure.list.byHospitalization")
@Service
public class ListHospitalizationProceduresByHospitalizationService
        implements ListHospitalizationProceduresByHospitalizationUseCase {
    private final HospitalizationProcedureRepository repository;

    public ListHospitalizationProceduresByHospitalizationService(HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HospitalizationProcedureDto> listByHospitalization(Long hospitalizationId) {
        return repository.findAllByHospitalizationId(hospitalizationId).stream()
            .map(HospitalizationProcedureDto::from).toList();
    }
}
