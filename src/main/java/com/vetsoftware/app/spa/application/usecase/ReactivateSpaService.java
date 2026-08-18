package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.ReactivateSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.reactivate")
@Service
public class ReactivateSpaService implements ReactivateSpaUseCase {
    private final SpaRepository repository;

    public ReactivateSpaService(SpaRepository repository) {
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
    public SpaDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new SpaNotFoundException(id);
        return SpaDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new SpaNotFoundException(id)));
    }
}
