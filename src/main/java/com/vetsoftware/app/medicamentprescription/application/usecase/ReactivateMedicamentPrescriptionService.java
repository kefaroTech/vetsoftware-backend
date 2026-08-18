package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.ReactivateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.prescription.reactivate")
@Service
public class ReactivateMedicamentPrescriptionService
        implements
            ReactivateMedicamentPrescriptionUseCase {
    private final MedicamentPrescriptionRepository repository;

    public ReactivateMedicamentPrescriptionService(MedicamentPrescriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura. Aqui no hay un
     * findById previo que valide la propiedad —el servicio decide si existe mirando
     * las filas afectadas—, asi que la consulta acotada es la unica barrera: la
     * correccion de {@code create} no habia llegado hasta aqui. Cero filas
     * significa «no existe en TU empresa», que es tambien la respuesta correcta
     * para la linea de otro tenant: un 404, sin revelar que el id existe.
     *
     * <p>
     * {@code companyId == null} es el camino SYSTEM (el controller lo pone con
     * {@code currentCompanyIdOrNull()}), que si puede operar sin acotar.
     */
    @Override
    @Transactional
    public MedicamentPrescriptionDto execute(Long id, Long companyId) {
        int rows = companyId == null
                ? repository.reactivate(id)
                : repository.reactivate(id, companyId);
        if (rows == 0)
            throw new MedicamentPrescriptionNotFoundException(id);
        return MedicamentPrescriptionDto.from((companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new MedicamentPrescriptionNotFoundException(id)));
    }
}
