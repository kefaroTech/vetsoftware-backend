package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.PageResult;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ListHospitalizationProceduresByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.procedure.list.by.hospitalization")
@Service
public class ListHospitalizationProceduresByHospitalizationService
        implements
            ListHospitalizationProceduresByHospitalizationUseCase {
    private final HospitalizationProcedureRepository repository;

    public ListHospitalizationProceduresByHospitalizationService(
            HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<HospitalizationProcedureDto> listByHospitalization(Long hospitalizationId,
            Long companyId, int page, int pageSize) {
        return repository.findAllByHospitalizationIdAndCompanyId(hospitalizationId, companyId, page,
                pageSize).map(HospitalizationProcedureDto::from);
    }
}
