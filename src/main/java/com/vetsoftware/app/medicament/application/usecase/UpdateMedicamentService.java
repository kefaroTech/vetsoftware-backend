package com.vetsoftware.app.medicament.application.usecase;

import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.UpdateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.update")
@Service
public class UpdateMedicamentService implements UpdateMedicamentUseCase {
  private final MedicamentRepository repository;

  public UpdateMedicamentService(MedicamentRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public MedicamentDto execute(UpdateMedicamentCommand command) {
    Medicament medicament =
        repository
            .findById(command.id())
            .orElseThrow(() -> new MedicamentNotFoundException(command.id()));
    // Solo nombre/descripción; se conserva el scope (general/empresa) del medicamento.
    medicament.update(
        command.name(), command.description(), medicament.getCompany(), medicament.isGeneral());
    return MedicamentDto.from(repository.save(medicament));
  }
}
