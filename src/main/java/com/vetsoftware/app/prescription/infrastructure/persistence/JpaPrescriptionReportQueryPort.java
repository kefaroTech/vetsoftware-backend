package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository.LatestWeightProjection;
import com.vetsoftware.app.prescription.application.dto.PrescriptionSignalment;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionReportQueryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga clínica + paciente + propietario para la fórmula. Cruce cross-feature
 * permitido: solo importa
 * {@code AnimalJpaRepository}/{@code WeightRecordJpaRepository} (persistencia).
 */
@Component
public class JpaPrescriptionReportQueryPort implements PrescriptionReportQueryPort {

    private final AnimalJpaRepository animalJpaRepository;
    private final WeightRecordJpaRepository weightRecordJpaRepository;

    public JpaPrescriptionReportQueryPort(AnimalJpaRepository animalJpaRepository,
            WeightRecordJpaRepository weightRecordJpaRepository) {
        this.animalJpaRepository = animalJpaRepository;
        this.weightRecordJpaRepository = weightRecordJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PrescriptionSignalment> loadByAnimal(Long animalId, Long companyId) {
        return animalJpaRepository.findByIdAndCompany_Id(animalId, companyId)
                .map(a -> toSignalment(a, companyId));
    }

    private PrescriptionSignalment toSignalment(AnimalJpaEntity a, Long companyId) {
        LatestWeightProjection weight = weightRecordJpaRepository
                .findLatestByAnimalId(a.getId(), companyId).orElse(null);
        var owner = a.getOwner();
        var company = a.getCompany();
        return new PrescriptionSignalment(company.getName(), company.getIdentifier(),
                company.getAddress(), company.getContactNumber(),
                company.getCity() != null ? company.getCity().getName() : null, a.getName(),
                a.getCode(), a.getSpecie().getName(), a.getBreed().getName(),
                genderLabel(a.getGender() == null ? null : a.getGender().name()),
                a.getColor() != null ? a.getColor().getName() : null, ageLabel(a.getBod()),
                weight != null ? formatWeight(weight.getValue(), weight.getUnit()) : null,
                owner.getName(), owner.getDocument(), owner.getPhone(), owner.getEmail(),
                owner.getAddress());
    }

    private static String genderLabel(String gender) {
        if (gender == null)
            return null;
        return switch (gender) {
            case "MALE" -> "Macho";
            case "FEMALE" -> "Hembra";
            default -> gender;
        };
    }

    private static String formatWeight(BigDecimal value, String unit) {
        if (value == null)
            return null;
        String number = value.stripTrailingZeros().toPlainString();
        String suffix = switch (unit == null ? "" : unit) {
            case "GRAMS" -> "g";
            case "POUNDS" -> "lb";
            case "KILOGRAMS" -> "kg";
            default -> "";
        };
        return suffix.isEmpty() ? number : number + " " + suffix;
    }

    private static String ageLabel(LocalDate birthDate) {
        if (birthDate == null)
            return null;
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today))
            return null;
        Period p = Period.between(birthDate, today);
        int years = p.getYears();
        int months = p.getMonths();
        if (years >= 1) {
            return months > 0
                    ? years + " año" + (years == 1 ? "" : "s") + " y " + months + " mes"
                            + (months == 1 ? "" : "es")
                    : years + " año" + (years == 1 ? "" : "s");
        }
        if (months >= 1)
            return months + " mes" + (months == 1 ? "" : "es");
        int days = p.getDays();
        return days + " día" + (days == 1 ? "" : "s");
    }
}
