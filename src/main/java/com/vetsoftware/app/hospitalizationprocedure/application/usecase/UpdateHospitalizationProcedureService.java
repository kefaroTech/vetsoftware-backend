package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.command.UpdateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.UpdateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.procedure.update")
@Service
public class UpdateHospitalizationProcedureService
        implements
            UpdateHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;

    public UpdateHospitalizationProcedureService(HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationProcedureDto execute(UpdateHospitalizationProcedureCommand command) {
        HospitalizationProcedure procedure = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new HospitalizationProcedureNotFoundException(command.id()));
        procedure.update(command.name(), command.dose(), parseFrequency(command.frequency()),
                parseGuidelineType(command.guidelineType()),
                parseDurationMeasure(command.durationMeasure()), command.durationQuantity(),
                command.startDate(), command.startTime(), command.notes());
        return HospitalizationProcedureDto.from(repository.save(procedure));
    }

    private Frequency parseFrequency(String s) {
        return s == null || s.isBlank() ? null : Frequency.valueOf(s.trim().toUpperCase());
    }

    private GuidelineType parseGuidelineType(String s) {
        return s == null || s.isBlank() ? null : GuidelineType.valueOf(s.trim().toUpperCase());
    }

    private DurationMeasure parseDurationMeasure(String s) {
        return s == null || s.isBlank() ? null : DurationMeasure.valueOf(s.trim().toUpperCase());
    }
}
