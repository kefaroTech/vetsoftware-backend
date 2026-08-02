package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.port.in.DeleteSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.delete")
@Service
public class DeleteSpaService implements DeleteSpaUseCase {
  private final SpaRepository repository;

  public DeleteSpaService(SpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    (companyId == null ? repository.findById(id) : repository.findByIdAndCompanyId(id, companyId))
        .orElseThrow(() -> new SpaNotFoundException(id));
    repository.delete(id);
  }
}
