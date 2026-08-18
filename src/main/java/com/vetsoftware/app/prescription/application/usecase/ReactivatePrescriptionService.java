package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.ReactivatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "prescription.reactivate")
@Service
public class ReactivatePrescriptionService implements ReactivatePrescriptionUseCase {
    private final PrescriptionRepository repository;

    public ReactivatePrescriptionService(PrescriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura. Aqui no hay un
     * findById previo que valide la propiedad —el servicio decide si existe mirando
     * las filas afectadas—, asi que la consulta acotada es la unica barrera. Cero
     * filas significa «no existe en TU empresa», que es tambien la respuesta
     * correcta para la receta de otro tenant: un 404, sin revelar que el id existe.
     *
     * <p>
     * {@code companyId == null} es el camino SYSTEM (el controller lo pone con
     * {@code currentCompanyIdOrNull()}), que si puede operar sin acotar.
     */
    @Override
    @Transactional
    public PrescriptionDto execute(Long id, Long companyId) {
        int rows = companyId == null
                ? repository.reactivate(id)
                : repository.reactivate(id, companyId);
        if (rows == 0)
            throw new PrescriptionNotFoundException(id);
        return PrescriptionDto.from((companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new PrescriptionNotFoundException(id)));
    }
}
