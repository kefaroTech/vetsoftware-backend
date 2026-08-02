package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "electronic.document.find")
@Service
public class FindElectronicDocumentService implements FindElectronicDocumentUseCase {
  private final ElectronicDocumentRepository repository;

  public FindElectronicDocumentService(ElectronicDocumentRepository repository) {
    this.repository = repository;
  }

  @Override
  public ElectronicDocumentDto findById(Long id, Long companyId) {
    return ElectronicDocumentDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ElectronicDocumentNotFoundException(id)));
  }
}
