package com.vetsoftware.app.medicament.application.port.out;

public interface MedicamentPrescriptionChildrenQueryPort {
  boolean existsActiveByMedicamentId(Long medicamentId);
}
