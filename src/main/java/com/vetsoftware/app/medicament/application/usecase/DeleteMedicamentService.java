package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.port.in.DeleteMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
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

  public DeleteMedicamentService(
      MedicamentRepository repository, MedicamentPrescriptionChildrenQueryPort childrenQueryPort) {
    this.repository = repository;
    this.childrenQueryPort = childrenQueryPort;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new MedicamentNotFoundException(id));
    if (childrenQueryPort.existsActiveByMedicamentId(id)) {
      throw new MedicamentHasActiveChildrenException(id, "medicamentPrescription");
    }
    repository.delete(id);
  }
}
