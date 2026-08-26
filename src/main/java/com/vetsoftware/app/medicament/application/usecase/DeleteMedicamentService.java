package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.port.in.DeleteMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentHasActiveChildrenException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.delete")
@Service
public class DeleteMedicamentService implements DeleteMedicamentUseCase {
    private final MedicamentRepository repository;
    private final MedicamentPrescriptionChildrenQueryPort childrenQueryPort;

    public DeleteMedicamentService(MedicamentRepository repository,
            MedicamentPrescriptionChildrenQueryPort childrenQueryPort) {
        this.repository = repository;
        this.childrenQueryPort = childrenQueryPort;
    }

    /**
     * La comprobacion de existencia va acotada a la empresa: es lo unico que separa
     * un 404 de borrar el vademecum de otro tenant. {@code companyId == null} es el
     * camino SYSTEM.
     *
     * <p>
     * Ese camino filtra ademas por {@code isGeneral}, y el filtro no es defensa en
     * profundidad: es la barrera (#590). Un principal de plataforma no tiene
     * empresa que acotar, asi que sin el, un DELETE con el id del medicamento
     * PRIVADO de una clinica lo encontraba, pasaba la comprobacion de recetas y lo
     * daba de baja logica: 204, la fila con {@code enabled = false} por el
     * {@code @SQLDelete}, y la clinica dejando de verlo en su catalogo sin una sola
     * traza. Es el espejo exacto del motivo por el que el camino del empleado usa
     * el finder de lo PROPIO. Un 404 y no un 403: no se revela de quien es la fila.
     *
     * <p>
     * Hoy el controller del tenant pasa siempre una empresa, asi que la rama es
     * inalcanzable desde HTTP; se blinda igualmente porque la administracion global
     * vive ya en {@code DeleteGlobalMedicamentService} y este ternario es lo que
     * quedaria abierto el dia que alguien mueva este controller a
     * {@code currentCompanyIdOrNull()}.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id).filter(Medicament::isGeneral)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new MedicamentNotFoundException(id));
        if (childrenQueryPort.existsActiveByMedicamentId(id)) {
            throw new MedicamentHasActiveChildrenException(id, "medicamentPrescription");
        }
        repository.delete(id);
    }
}
