package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.port.in.DeleteGlobalMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentHasActiveChildrenException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pausa un medicamento del vademecum de PLATAFORMA. Es baja logica: el
 * {@code @SQLDelete} de la entidad lo deja con {@code enabled = false}, fuera
 * del catalogo activo y recuperable desde
 * {@code GET /admin/medicaments/disabled}.
 *
 * <p>
 * El {@code filter(Medicament::isGeneral)} es la barrera, por el mismo motivo
 * que en {@link UpdateGlobalMedicamentService} y con la consecuencia mas fea:
 * sin el, un DELETE de plataforma con el id del medicamento PRIVADO de una
 * clinica devolveria 204, lo pausaria, y la clinica dejaria de verlo en su
 * catalogo sin una sola traza de que hubiera pasado nada.
 */
@Observed(name = "medicament.global.delete")
@Service
public class DeleteGlobalMedicamentService implements DeleteGlobalMedicamentUseCase {
    private final MedicamentRepository repository;
    private final MedicamentPrescriptionChildrenQueryPort childrenQueryPort;

    public DeleteGlobalMedicamentService(MedicamentRepository repository,
            MedicamentPrescriptionChildrenQueryPort childrenQueryPort) {
        this.repository = repository;
        this.childrenQueryPort = childrenQueryPort;
    }

    /**
     * La comprobacion de recetas activas no se acota a ninguna empresa a proposito:
     * un global lo receta cualquier tenant, asi que pausarlo mientras alguna receta
     * viva lo referencia dejaria esa receta apuntando a un medicamento que ya no
     * esta en ningun catalogo.
     */
    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).filter(Medicament::isGeneral)
                .orElseThrow(() -> new MedicamentNotFoundException(id));
        if (childrenQueryPort.existsActiveByMedicamentId(id)) {
            throw new MedicamentHasActiveChildrenException(id, "medicamentPrescription");
        }
        repository.delete(id);
    }
}
