package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaEntity;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaRepository;
import com.vetsoftware.app.procedureschedule.application.port.out.HospitalizationProcedureQueryPort;
import com.vetsoftware.app.procedureschedule.domain.ProcedureOrderParams;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaHospitalizationProcedureQueryPort implements HospitalizationProcedureQueryPort {
    private final HospitalizationProcedureJpaRepository procedureJpaRepository;

    public JpaHospitalizationProcedureQueryPort(
            HospitalizationProcedureJpaRepository procedureJpaRepository) {
        this.procedureJpaRepository = procedureJpaRepository;
    }

    @Override
    public Optional<ProcedureOrderParams> findById(Long hospitalizationProcedureId) {
        return procedureJpaRepository.findById(hospitalizationProcedureId).map(this::toParams);
    }

    /**
     * {@code findByIdAndHospitalization_Company_Id} es el JOIN que sube de la orden
     * a la hospitalizacion y de ahi a la empresa; el {@code @EntityGraph} del
     * repositorio ya trae la hospitalizacion, asi que sigue siendo una sola query.
     */
    @Override
    public Optional<ProcedureOrderParams> findByIdAndCompanyId(Long hospitalizationProcedureId,
            Long companyId) {
        return procedureJpaRepository
                .findByIdAndHospitalization_Company_Id(hospitalizationProcedureId, companyId)
                .map(this::toParams);
    }

    private ProcedureOrderParams toParams(HospitalizationProcedureJpaEntity p) {
        return new ProcedureOrderParams(p.getId(), p.getName(), p.getHospitalization().getId(),
                p.getFrequency(), p.getGuidelineType(), p.getDurationMeasure(),
                p.getDurationQuantity(), p.getStartDate(), p.getStartTime());
    }
}
