package com.vetsoftware.app.medicamentprescription.application.port.out;

import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import java.util.List;
import java.util.Optional;

public interface MedicamentPrescriptionRepository {
  MedicamentPrescription save(MedicamentPrescription medicament);

  Optional<MedicamentPrescription> findById(Long id);

  Optional<MedicamentPrescription> findByIdAndCompanyId(Long id, Long companyId);

  List<MedicamentPrescription> findAll();

  void delete(Long id);

  int reactivate(Long id);
}
