package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.ReactivateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.reactivate")
@Service
public class ReactivateSurgeryService implements ReactivateSurgeryUseCase {
    private final SurgeryRepository repository;

    public ReactivateSurgeryService(SurgeryRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para el registro
     * de otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public SurgeryDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new SurgeryNotFoundException(id);
        return SurgeryDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new SurgeryNotFoundException(id)));
    }
}
