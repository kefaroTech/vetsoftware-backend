package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.ReactivateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "daycare.reactivate")
@Service
public class ReactivateDayCareService implements ReactivateDayCareUseCase {
    private final DayCareRepository repository;

    public ReactivateDayCareService(DayCareRepository repository) {
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
    public DayCareDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new DayCareNotFoundException(id);
        return DayCareDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new DayCareNotFoundException(id)));
    }
}
