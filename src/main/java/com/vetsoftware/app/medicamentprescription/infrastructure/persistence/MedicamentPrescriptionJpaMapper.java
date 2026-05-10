package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicamentPrescriptionJpaMapper {

    public MedicamentPrescriptionJpaEntity toJpa(MedicamentPrescription medicament,
                                                 PrescriptionJpaEntity prescription) {
        MedicamentPrescriptionJpaEntity entity = new MedicamentPrescriptionJpaEntity();
        entity.setId(medicament.getId());
        entity.setName(medicament.getName());
        entity.setPresentation(medicament.getPresentation());
        entity.setQuantity(medicament.getQuantity());
        entity.setPosology(medicament.getPosology());
        entity.setPrescription(prescription);
        entity.setCreatedDate(medicament.getCreatedDate());
        return entity;
    }

    public MedicamentPrescription toDomain(MedicamentPrescriptionJpaEntity entity) {
        PrescriptionJpaEntity p = entity.getPrescription();
        return toDomain(entity, new PrescriptionRef(p.getId(), p.getDate()));
    }

    public MedicamentPrescription toDomain(MedicamentPrescriptionJpaEntity entity,
                                           PrescriptionRef prescriptionRef) {
        return new MedicamentPrescription(
            entity.getId(), entity.getName(), entity.getPresentation(),
            entity.getQuantity(), entity.getPosology(),
            prescriptionRef, entity.getCreatedDate());
    }
}
