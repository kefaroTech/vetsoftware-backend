package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.ListElectronicDocumentsUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "electronicDocument.list")
@Service
public class ListElectronicDocumentsService implements ListElectronicDocumentsUseCase {
    private final ElectronicDocumentRepository repository;

    public ListElectronicDocumentsService(ElectronicDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ElectronicDocumentDto> listByCompany(Long companyId, Long branchId) {
        return repository.findAllByCompanyId(companyId, branchId).stream()
                .map(ElectronicDocumentDto::from).toList();
    }
}
