package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.FindMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "medicament.find")
@Service
public class FindMedicamentService implements FindMedicamentUseCase {
    private final MedicamentRepository repository;

    public FindMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    /**
     * Lectura, no escritura: aqui si valen los generales de la plataforma ademas de
     * los propios, que es lo que la pantalla de receta necesita ver. Las escrituras
     * usan {@code findByIdAndCompanyId}, que solo alcanza lo propio.
     */
    @Override
    public MedicamentDto findById(Long id, Long companyId) {
        return MedicamentDto.from(repository.findAvailableByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new MedicamentNotFoundException(id)));
    }
}
