package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.dto.PageResult;
import com.vetsoftware.app.electronicdocument.application.port.in.ListElectronicDocumentsUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "electronic.document.list")
@Service
public class ListElectronicDocumentsService implements ListElectronicDocumentsUseCase {
    private final ElectronicDocumentRepository repository;

    public ListElectronicDocumentsService(ElectronicDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<ElectronicDocumentDto> listByCompany(Long companyId, Long branchId,
            ElectronicDocumentType documentType, DianStatus dianStatus, int page, int pageSize) {
        return repository
                .findAllByCompanyId(companyId, branchId, documentType, dianStatus, page, pageSize)
                .map(ElectronicDocumentDto::from);
    }
}
