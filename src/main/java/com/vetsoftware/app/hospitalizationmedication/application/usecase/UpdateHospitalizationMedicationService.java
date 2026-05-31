package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.UpdateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization_medication.update")
@Service
public class UpdateHospitalizationMedicationService implements UpdateHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;

    public UpdateHospitalizationMedicationService(HospitalizationMedicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationMedicationDto execute(UpdateHospitalizationMedicationCommand command) {
        HospitalizationMedication medication = repository.findById(command.id())
            .orElseThrow(() -> new HospitalizationMedicationNotFoundException(command.id()));
        medication.update(
            command.name(),
            command.dose(),
            parseFrequency(command.frequency()),
            parseGuidelineType(command.guidelineType()),
            parseDurationMeasure(command.durationMeasure()),
            command.durationQuantity(),
            command.startDate(),
            command.startTime(),
            command.notes());
        return HospitalizationMedicationDto.from(repository.save(medication));
    }

    private static Frequency parseFrequency(String s) {
        return s == null || s.isBlank() ? null : Frequency.valueOf(s.trim().toUpperCase());
    }

    private static GuidelineType parseGuidelineType(String s) {
        return s == null || s.isBlank() ? null : GuidelineType.valueOf(s.trim().toUpperCase());
    }

    private static DurationMeasure parseDurationMeasure(String s) {
        return s == null || s.isBlank() ? null : DurationMeasure.valueOf(s.trim().toUpperCase());
    }
}
