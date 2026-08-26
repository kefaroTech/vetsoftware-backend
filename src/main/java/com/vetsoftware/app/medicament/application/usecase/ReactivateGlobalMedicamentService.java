package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.ReactivateGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reactiva un medicamento GLOBAL pausado.
 *
 * <p>
 * Va por {@code reactivateGlobal(id)} y no por {@code reactivate(id, null)}
 * porque el segundo no reactivaria NADA: su {@code WHERE} es
 * {@code company_id = :companyId} y en SQL {@code company_id = NULL} no casa
 * nunca, ni siquiera con las filas que tienen esa columna nula. El sintoma era
 * un 404 permanente sobre una fila que existe —es decir, un global pausado
 * irrecuperable—, y el arreglo copia el precedente que ya vivia en el mismo
 * repositorio: {@code reactivateWithDetails(id, name, description)}, con
 * {@code company_id IS NULL} en el {@code WHERE}.
 *
 * <p>
 * Igual que en el camino del tenant, aqui no hay lectura previa que valide
 * nada: el numero de filas afectadas ES la comprobacion de existencia, y ese
 * {@code company_id IS NULL} es lo unico que impide que este caso de uso
 * resucite el medicamento privado de un tenant.
 */
@Observed(name = "medicament.global.reactivate")
@Service
public class ReactivateGlobalMedicamentService implements ReactivateGlobalMedicamentUseCase {
    private final MedicamentRepository repository;

    public ReactivateGlobalMedicamentService(MedicamentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public MedicamentDto execute(Long id) {
        int rows = repository.reactivateGlobal(id);
        if (rows == 0) {
            throw new MedicamentNotFoundException(id);
        }
        return MedicamentDto.from(repository.findById(id).filter(Medicament::isGeneral)
                .orElseThrow(() -> new MedicamentNotFoundException(id)));
    }
}
