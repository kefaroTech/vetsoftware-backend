package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentByAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Observed(name = "electronic.document.find.by.account")
@Service
public class FindElectronicDocumentByAccountService
        implements
            FindElectronicDocumentByAccountUseCase {
    private final ElectronicDocumentRepository repository;

    public FindElectronicDocumentByAccountService(ElectronicDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ElectronicDocumentDto> findByOpenAccount(Long openAccountId, Long companyId) {
        return repository.findByOpenAccountId(openAccountId, companyId)
                .map(ElectronicDocumentDto::from);
    }
}
