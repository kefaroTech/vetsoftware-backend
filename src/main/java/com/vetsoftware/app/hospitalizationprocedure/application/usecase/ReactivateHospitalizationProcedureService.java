package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ReactivateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.procedure.reactivate")
@Service
public class ReactivateHospitalizationProcedureService
        implements
            ReactivateHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;

    public ReactivateHospitalizationProcedureService(
            HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para la orden de
     * otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public HospitalizationProcedureDto execute(Long id, Long companyId) {
        int updated = repository.reactivate(id, companyId);
        if (updated == 0)
            throw new HospitalizationProcedureNotFoundException(id);
        return HospitalizationProcedureDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationProcedureNotFoundException(id)));
    }
}
