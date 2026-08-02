package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.DeleteHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.progress.note.delete")
@Service
public class DeleteHospitalizationProgressNoteService
    implements DeleteHospitalizationProgressNoteUseCase {
  private final HospitalizationProgressNoteRepository repository;

  public DeleteHospitalizationProgressNoteService(
      HospitalizationProgressNoteRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new HospitalizationProgressNoteNotFoundException(id));
    repository.delete(id);
  }
}
