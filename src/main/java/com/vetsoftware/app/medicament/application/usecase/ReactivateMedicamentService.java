package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ReactivateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.reactivate")
@Service
public class ReactivateMedicamentService implements ReactivateMedicamentUseCase {
    private final MedicamentRepository repository;

    public ReactivateMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para el
     * medicamento de otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public MedicamentDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new MedicamentNotFoundException(id);
        return MedicamentDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new MedicamentNotFoundException(id)));
    }
}
