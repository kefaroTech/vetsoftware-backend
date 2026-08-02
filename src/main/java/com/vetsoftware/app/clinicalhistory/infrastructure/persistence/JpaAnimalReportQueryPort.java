package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository.LatestWeightProjection;
import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.clinicalhistory.application.port.out.AnimalReportQueryPort;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaAnimalReportQueryPort implements AnimalReportQueryPort {

    private final AnimalJpaRepository animalJpaRepository;
    private final WeightRecordJpaRepository weightRecordJpaRepository;

    public JpaAnimalReportQueryPort(AnimalJpaRepository animalJpaRepository,
            WeightRecordJpaRepository weightRecordJpaRepository) {
        this.animalJpaRepository = animalJpaRepository;
        this.weightRecordJpaRepository = weightRecordJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnimalReportInfo> findByIdAndCompanyId(Long animalId, Long companyId) {
        return animalJpaRepository.findById(animalId)
                .filter(a -> a.getCompany() != null && companyId.equals(a.getCompany().getId()))
                .map(a -> toReportInfo(a, companyId));
    }

    private AnimalReportInfo toReportInfo(AnimalJpaEntity a, Long companyId) {
        LatestWeightProjection weight = weightRecordJpaRepository
                .findLatestByAnimalId(a.getId(), companyId).orElse(null);
        return new AnimalReportInfo(a.getId(), a.getName(), a.getCode(), a.getSpecie().getName(),
                a.getBreed().getName(), a.getColor() != null ? a.getColor().getName() : null,
                genderLabel(a.getGender() == null ? null : a.getGender().name()),
                reproductiveStateLabel(
                        a.getReproductiveState() == null ? null : a.getReproductiveState().name()),
                a.getBod(), ageLabel(a.getBod()),
                weight != null ? formatWeight(weight.getValue(), weight.getUnit()) : null,
                weight != null ? weight.getMeasuredAt() : null, a.isDeceased(), a.getDeceasedDate(),
                a.getOwner().getName(), a.getOwner().getDocument(), a.getOwner().getPhone(),
                a.getOwner().getEmail(), a.getOwner().getAddress(), a.getCompany().getName(),
                a.getCompany().getIdentifier(), a.getCompany().getAddress(),
                a.getCompany().getContactNumber());
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

    private static String reproductiveStateLabel(String state) {
        if (state == null)
            return null;
        return switch (state) {
            case "STERILIZED" -> "Esterilizado";
            case "NO_STERILIZED" -> "Entero (sin esterilizar)";
            case "UNKNOWN" -> "Sin determinar";
            default -> state;
        };
    }

    private static String formatWeight(java.math.BigDecimal value, String unit) {
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
        if (months >= 1) {
            return months + " mes" + (months == 1 ? "" : "es");
        }
        int days = p.getDays();
        return days + " día" + (days == 1 ? "" : "s");
    }
}
