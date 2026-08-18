package com.vetsoftware.app.vaccination.infrastructure.persistence;

import com.vetsoftware.app.vaccination.application.port.out.VaccinationTypeQueryPort;
import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;
import com.vetsoftware.app.vaccinationtype.infrastructure.persistence.VaccinationTypeJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("vaccinationJpaVaccinationTypeQueryPort")
public class JpaVaccinationTypeQueryPort implements VaccinationTypeQueryPort {
    private final VaccinationTypeJpaRepository vaccinationTypeJpaRepository;

    public JpaVaccinationTypeQueryPort(VaccinationTypeJpaRepository vaccinationTypeJpaRepository) {
        this.vaccinationTypeJpaRepository = vaccinationTypeJpaRepository;
    }

    @Override
    public Optional<VaccinationTypeRef> findAvailableByIdAndCompanyId(Long vaccinationTypeId,
            Long companyId) {
        return vaccinationTypeJpaRepository.findAvailableById(vaccinationTypeId, companyId)
                .map(e -> new VaccinationTypeRef(e.getId(), e.getName()));
    }
}
