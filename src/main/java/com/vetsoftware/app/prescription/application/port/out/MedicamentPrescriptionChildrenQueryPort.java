package com.vetsoftware.app.prescription.application.port.out;

public interface MedicamentPrescriptionChildrenQueryPort {
    boolean existsActiveByPrescriptionId(Long parentId);
}
