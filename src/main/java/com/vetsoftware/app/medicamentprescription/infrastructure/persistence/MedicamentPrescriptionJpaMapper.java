package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaEntity;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicamentPrescriptionJpaMapper {

    public MedicamentPrescriptionJpaEntity toJpa(MedicamentPrescription medicament,
            PrescriptionJpaEntity prescription, MedicamentJpaEntity medicamentCatalog) {
        MedicamentPrescriptionJpaEntity entity = new MedicamentPrescriptionJpaEntity();
        entity.setId(medicament.getId());
        entity.setName(medicament.getName());
        entity.setMedicament(medicamentCatalog);
        entity.setPresentation(medicament.getPresentation());
        entity.setQuantity(medicament.getQuantity());
        entity.setPosology(medicament.getPosology());
        entity.setObservation(medicament.getObservation());
        entity.setPrescription(prescription);
        entity.setCreatedDate(medicament.getCreatedDate());
        entity.setEnabled(medicament.isEnabled());
        return entity;
    }

    public MedicamentPrescription toDomain(MedicamentPrescriptionJpaEntity entity) {
        PrescriptionJpaEntity p = entity.getPrescription();
        // El id del proxy LAZY se lee sin inicializar; el nombre viene del snapshot de
        // la fila.
        MedicamentRef medicamentRef = new MedicamentRef(entity.getMedicament().getId(),
                entity.getName());
        return toDomain(entity, new PrescriptionRef(p.getId(), p.getDate()), medicamentRef);
    }

    public MedicamentPrescription toDomain(MedicamentPrescriptionJpaEntity entity,
            PrescriptionRef prescriptionRef, MedicamentRef medicamentRef) {
        return new MedicamentPrescription(entity.getId(), medicamentRef, entity.getPresentation(),
                entity.getQuantity(), entity.getPosology(), entity.getObservation(),
                prescriptionRef, entity.getCreatedDate(), entity.isEnabled());
    }
}
