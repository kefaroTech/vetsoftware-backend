package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ReactivateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.medication.reactivate")
@Service
public class ReactivateHospitalizationMedicationService
        implements
            ReactivateHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;

    public ReactivateHospitalizationMedicationService(
            HospitalizationMedicationRepository repository) {
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
    public HospitalizationMedicationDto execute(Long id, Long companyId) {
        int updated = repository.reactivate(id, companyId);
        if (updated == 0)
            throw new HospitalizationMedicationNotFoundException(id);
        return HospitalizationMedicationDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationMedicationNotFoundException(id)));
    }
}
